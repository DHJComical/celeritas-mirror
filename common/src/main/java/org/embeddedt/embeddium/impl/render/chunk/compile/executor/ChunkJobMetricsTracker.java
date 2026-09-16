package org.embeddedt.embeddium.impl.render.chunk.compile.executor;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMaps;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkTaskOutput;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;
import java.util.function.LongFunction;

public class ChunkJobMetricsTracker {
    public static final long OBSERVATION_COUNT_TIME = TimeUnit.SECONDS.toNanos(1);

    public record MetricStats(long avg, long max, long min) {
        public String toString(LongFunction<String> observationStringifier) {
            return "avg = " + observationStringifier.apply(avg)
                    + ", max = " + observationStringifier.apply(max)
                    + ", min = " + observationStringifier.apply(min);
        }

        @Override
        public @NotNull String toString() {
            return toString(String::valueOf);
        }
    }

    public static class MetricsData {
        private static final int MAX_OBSERVATIONS = 10000;

        /**
         * Smoothing factor for the exponential moving average of execution time. Small enough to smooth over the
         * large per-section variance (empty vs. dense sections), large enough to track a change in terrain within a
         * few dozen completed tasks.
         */
        private static final double EMA_ALPHA = 0.05;

        private final LongArrayList observations = new LongArrayList(MAX_OBSERVATIONS);
        private int nextInsertPoint = 0;

        private int observationsInLastTimeInterval;
        private int observationsInCurrentTimeInterval;

        private double emaNanos;
        private boolean hasEma;

        public void collect(long observation) {
            if (observations.size() < MAX_OBSERVATIONS) {
                observations.add(observation);
            } else {
                observations.set(nextInsertPoint++, observation);
                if (nextInsertPoint >= MAX_OBSERVATIONS) {
                    nextInsertPoint = 0;
                }
            }
            observationsInCurrentTimeInterval++;

            if (this.hasEma) {
                this.emaNanos += EMA_ALPHA * (observation - this.emaNanos);
            } else {
                this.emaNanos = observation;
                this.hasEma = true;
            }
        }

        public void flipInterval() {
            this.observationsInLastTimeInterval = this.observationsInCurrentTimeInterval;
            this.observationsInCurrentTimeInterval = 0;
        }

        public int getObservationsInLastTimeInterval() {
            return this.observationsInLastTimeInterval;
        }

        /**
         * {@return the exponential moving average of the observed execution time in nanoseconds, or {@code fallback}
         * if nothing has been observed yet}
         */
        public double getAverageNanos(double fallback) {
            return this.hasEma ? this.emaNanos : fallback;
        }

        public MetricStats getStats() {
            int count = observations.size();
            if (count == 0) {
                return new MetricStats(0, 0, 0);
            }
            var iter = observations.iterator();
            long sum = 0;
            long min = Long.MAX_VALUE;
            long max = Long.MIN_VALUE;
            while (iter.hasNext()) {
                long observation = iter.next();
                sum += observation;
                min = Math.min(min, observation);
                max = Math.max(max, observation);
            }
            return new MetricStats(sum / count, max, min);
        }
    }

    private final Reference2ReferenceMap<Class<? extends ChunkTaskOutput>, MetricsData> metricsByTask = new Reference2ReferenceArrayMap<>(2);

    private long lastTimeIntervalFlip = System.nanoTime();

    public void tick() {
        long time = System.nanoTime();
        if ((time - lastTimeIntervalFlip) >= OBSERVATION_COUNT_TIME) {
            for (var data : metricsByTask.values()) {
                data.flipInterval();
            }
            lastTimeIntervalFlip = time;
        }
    }

    public void collectMetrics(ChunkJobResult.Success<? extends ChunkTaskOutput> successfulResult) {
        if (successfulResult.executionTimeNanos() < 0) {
            return;
        }
        var data = metricsByTask.computeIfAbsent(successfulResult.output().getClass(), $ -> new MetricsData());
        data.collect(successfulResult.executionTimeNanos());
    }

    /**
     * {@return the recent average execution time in nanoseconds of tasks producing the given output type, or
     * {@code fallback} if none have completed yet}
     */
    public double getAverageExecutionNanos(Class<? extends ChunkTaskOutput> outputType, double fallback) {
        var data = metricsByTask.get(outputType);
        return data != null ? data.getAverageNanos(fallback) : fallback;
    }

    public Reference2ReferenceMap<Class<? extends ChunkTaskOutput>, MetricsData> getMetrics() {
        return Reference2ReferenceMaps.unmodifiable(metricsByTask);
    }
}
