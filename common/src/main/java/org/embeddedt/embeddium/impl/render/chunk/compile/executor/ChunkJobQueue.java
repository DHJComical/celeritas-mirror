package org.embeddedt.embeddium.impl.render.chunk.compile.executor;

import org.jetbrains.annotations.Nullable;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

class ChunkJobQueue {
    private final ConcurrentLinkedDeque<ChunkJob> jobs = new ConcurrentLinkedDeque<>();

    private final Semaphore semaphore = new Semaphore(0);

    private final AtomicBoolean isRunning = new AtomicBoolean(true);

    // Set by a worker whenever it has to block waiting for work, i.e. the queue ran dry. Read and cleared once
    // per frame by the scheduling controller to detect under-provisioning of the in-flight target.
    private final AtomicBoolean workerBlocked = new AtomicBoolean(false);

    public boolean isRunning() {
        return this.isRunning.get();
    }

    public void add(ChunkJob job, boolean important) {
        if (!this.isRunning()) {
            throw new IllegalStateException("Queue is no longer running");
        }

        if (important) {
            this.jobs.addFirst(job);
        } else {
            this.jobs.addLast(job);
        }

        this.semaphore.release(1);
    }

    @Nullable
    public ChunkJob pollJob() {
        if (this.isRunning() && this.semaphore.tryAcquire()) {
            return this.getNextTask();
        } else {
            return null;
        }
    }

    @Nullable
    public ChunkJob waitForNextJob() throws InterruptedException {
        if (!this.isRunning()) {
            return null;
        }

        if (!this.semaphore.tryAcquire()) {
            // No work was immediately available, so we are about to block. Record this so the scheduler can grow
            // the in-flight target and keep us fed on subsequent frames.
            this.workerBlocked.set(true);
            this.semaphore.acquire();
        }

        return this.getNextTask();
    }

    /**
     * {@return whether a worker has blocked on an empty queue since this method was last called, atomically
     * clearing the flag for the next window}
     */
    public boolean checkAndClearWorkerBlocked() {
        return this.workerBlocked.getAndSet(false);
    }

    public boolean stealJob(ChunkJob job) {
        if (!this.semaphore.tryAcquire()) {
            return false;
        }

        var success = this.jobs.remove(job);

        if (!success) {
            // If we didn't manage to actually steal the task, then we need to release the permit which we did steal
            this.semaphore.release(1);
        }

        return success;
    }

    @Nullable
    private ChunkJob getNextTask() {
        return this.jobs.poll();
    }


    public Collection<ChunkJob> shutdown() {
        var list = new ArrayDeque<ChunkJob>();

        this.isRunning.set(false);

        while (this.semaphore.tryAcquire()) {
            var task = this.jobs.poll();

            if (task != null) {
                list.add(task);
            }
        }

        // force the worker threads to wake up and exit
        this.semaphore.release(Runtime.getRuntime().availableProcessors());

        return list;
    }

    public int size() {
        return this.semaphore.availablePermits();
    }

    public boolean isEmpty() {
        return this.size() == 0;
    }
}
