package org.embeddedt.embeddium.impl.render.chunk.compile.executor;

import java.util.Objects;

public sealed interface ChunkJobResult<OUTPUT> permits ChunkJobResult.Success, ChunkJobResult.Failure {
    record Success<OUTPUT>(OUTPUT output, long executionTimeNanos) implements ChunkJobResult<OUTPUT> {

    }

    record Failure<OUTPUT>(Throwable throwable) implements ChunkJobResult<OUTPUT> {
        public Failure {
            Objects.requireNonNull(throwable);
        }

        public void abort() {
            if (this.throwable instanceof RuntimeException crashException) {
                // Propagate RuntimeExceptions directly to provide extra information if they are a vanilla crash exception
                throw crashException;
            } else {
                throw new RuntimeException("Exception thrown while executing job", this.throwable);
            }
        }
    }
}
