package org.embeddedt.embeddium.impl.render.chunk.compile.executor;

import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkTaskOutput;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class ChunkJobCollector {
    private final Semaphore semaphore = new Semaphore(0);
    private final Consumer<ChunkJobResult<? extends ChunkTaskOutput>> collector;
    private final List<ChunkJob> submitted = new ArrayList<>();

    private final int budget;

    public ChunkJobCollector(int budget, Consumer<ChunkJobResult<? extends ChunkTaskOutput>> collector) {
        this.budget = budget;
        this.collector = collector;
    }

    public void onJobFinished(ChunkJobResult<? extends ChunkTaskOutput> result) {
        this.semaphore.release(1);
        this.collector.accept(result);
    }

    public void awaitCompletion(ChunkBuilder builder) {
        if (this.submitted.size() == 0) {
            return;
        }

        for (var job : this.submitted) {
            if (job.isStarted() || job.isCancelled()) {
                continue;
            }

            builder.tryStealTask(job);
        }

        // Acquire all the permits while running the managed block logic (to handle tasks requiring input from
        // the main thread)
        int remaining = this.submitted.size();
        BooleanSupplier isDone = () -> this.semaphore.availablePermits() > 0;

        while (remaining > 0) {
            int avail = this.semaphore.availablePermits();
            if (avail > 0) {
                int toTake = Math.min(avail, remaining);
                if (this.semaphore.tryAcquire(toTake)) {
                    remaining -= toTake;
                    continue;
                }
            }

            // otherwise, help by running a task
            builder.managedBlock(isDone);
        }
    }

    public void addSubmittedJob(ChunkJob job) {
        this.submitted.add(job);
    }

    public boolean canOffer() {
        return (this.budget - this.submitted.size()) > 0;
    }
}
