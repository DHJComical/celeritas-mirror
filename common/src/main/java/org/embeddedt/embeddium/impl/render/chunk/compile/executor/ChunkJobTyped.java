package org.embeddedt.embeddium.impl.render.chunk.compile.executor;

import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildContext;
import org.embeddedt.embeddium.impl.render.chunk.compile.tasks.ChunkBuilderTask;

import java.util.function.Consumer;

public class ChunkJobTyped<TASK extends ChunkBuilderTask<OUTPUT>, OUTPUT>
        implements ChunkJob
{
    private final TASK task;
    private final Consumer<ChunkJobResult<OUTPUT>> consumer;

    private volatile boolean cancelled;
    private volatile boolean started;

    ChunkJobTyped(TASK task, Consumer<ChunkJobResult<OUTPUT>> consumer) {
        this.task = task;
        this.consumer = consumer;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled() {
        this.cancelled = true;
    }

    @Override
    public void execute(ChunkBuildContext context) {
        // Task was cancelled before starting
        if (this.cancelled) {
            return;
        }

        this.started = true;

        ChunkJobResult<OUTPUT> result;

        long startTime = System.nanoTime();

        try {
            var output = this.task.execute(context, this);

            // Task was cancelled while executing
            if (output == null) {
                return;
            }

            result = new ChunkJobResult.Success<>(output, System.nanoTime() - startTime);
        } catch (Throwable throwable) {
            result = new ChunkJobResult.Failure<>(throwable);
            ChunkBuilder.LOGGER.error("Chunk build failed", throwable);
        }

        try {
            this.consumer.accept(result);
        } catch (Throwable throwable) {
            throw new RuntimeException("Exception while consuming result", throwable);
        }
    }

    @Override
    public boolean isStarted() {
        return this.started;
    }
}
