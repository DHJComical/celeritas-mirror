package org.embeddedt.embeddium.impl.render.chunk.compile.executor;

import java.util.Objects;

public sealed interface ChunkJobResult<OUTPUT> permits ChunkJobResult.Success, ChunkJobResult.Failure {
    record Success<OUTPUT>(OUTPUT output, long executionTimeNanos) implements ChunkJobResult<OUTPUT> {

    }

    record Failure<OUTPUT>(Throwable throwable) implements ChunkJobResult<OUTPUT> {
        public Failure {
            Objects.requireNonNull(throwable);
        }

        public RuntimeException asException() {
            if (this.throwable instanceof RuntimeException crashException) {
                return crashException;
            } else {
                return new RuntimeException("Exception thrown while executing job", this.throwable);
            }
        }
    }
}
