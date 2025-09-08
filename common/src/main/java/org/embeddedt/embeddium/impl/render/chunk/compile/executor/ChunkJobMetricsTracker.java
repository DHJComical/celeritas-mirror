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

        private final LongArrayList observations = new LongArrayList(MAX_OBSERVATIONS);
        private int nextInsertPoint = 0;

        private int observationsInLastTimeInterval;
        private int observationsInCurrentTimeInterval;

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
        }

        public int getObservationsInLastTimeInterval() {
            return this.observationsInLastTimeInterval;
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
                data.observationsInLastTimeInterval = data.observationsInCurrentTimeInterval;
                data.observationsInCurrentTimeInterval = 0;
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

    public Reference2ReferenceMap<Class<? extends ChunkTaskOutput>, MetricsData> getMetrics() {
        return Reference2ReferenceMaps.unmodifiable(metricsByTask);
    }
}
