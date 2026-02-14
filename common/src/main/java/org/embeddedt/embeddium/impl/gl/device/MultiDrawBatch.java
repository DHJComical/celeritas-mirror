package org.embeddedt.embeddium.impl.gl.device;

import static org.taumc.celeritas.lwjgl.LWJGLServiceProvider.LWJGL;
import org.taumc.celeritas.lwjgl.LWJGLServiceProvider;
import java.nio.IntBuffer;

/**
 * Provides a fixed-size queue for building a draw-command list usable with
 * {@link org.lwjgl.opengl.GL33#glMultiDrawElementsBaseVertex(int, IntBuffer, int, PointerBuffer, IntBuffer)}.
 */
public final class MultiDrawBatch {
    public final long pElementPointer;
    public final long pElementCount;
    public final long pBaseVertex;

    private final int capacity;

    public int size;

    public MultiDrawBatch(int capacity) {
        this.pElementPointer = LWJGL.nmemAlignedAlloc(32, (long) capacity * LWJGL.getPointerSize());
        if (this.pElementPointer == LWJGLServiceProvider.NULL) {
            throw new OutOfMemoryError("Failed to allocate element pointer array");
        }
        LWJGL.memSet(this.pElementPointer, 0x0, (long) capacity * LWJGL.getPointerSize());

        this.pElementCount = LWJGL.nmemAlignedAlloc(32, (long) capacity * Integer.BYTES);
        if (this.pElementCount == LWJGLServiceProvider.NULL) {
            LWJGL.nmemAlignedFree(this.pElementPointer);
            throw new OutOfMemoryError("Failed to allocate element count array");
        }
        this.pBaseVertex = LWJGL.nmemAlignedAlloc(32, (long) capacity * Integer.BYTES);
        if (this.pBaseVertex == LWJGLServiceProvider.NULL) {
            LWJGL.nmemAlignedFree(this.pElementPointer);
            LWJGL.nmemAlignedFree(this.pElementCount);
            throw new OutOfMemoryError("Failed to allocate base vertex array");
        }

        this.capacity = capacity;
    }

    public int size() {
        return this.size;
    }

    public int capacity() {
        return this.capacity;
    }

    public void clear() {
        this.size = 0;
    }

    public void delete() {
        LWJGL.nmemAlignedFree(this.pElementPointer);
        LWJGL.nmemAlignedFree(this.pElementCount);
        LWJGL.nmemAlignedFree(this.pBaseVertex);
    }

    public boolean isEmpty() {
        return this.size <= 0;
    }

    public int getIndexBufferSize() {
        int elements = 0;

        for (var index = 0; index < this.size; index++) {
            elements = Math.max(elements, LWJGL.memGetInt(this.pElementCount + ((long) index * Integer.BYTES)));
        }

        return elements;
    }
}
