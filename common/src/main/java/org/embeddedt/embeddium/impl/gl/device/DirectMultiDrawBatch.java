package org.embeddedt.embeddium.impl.gl.device;

import org.embeddedt.embeddium.impl.gl.tessellation.GlIndexType;
import org.embeddedt.embeddium.impl.gl.tessellation.GlPrimitiveType;
import org.embeddedt.embeddium.impl.gl.tessellation.GlTessellation;

import static org.taumc.celeritas.lwjgl.LWJGLServiceProvider.LWJGL;
import org.taumc.celeritas.lwjgl.LWJGLServiceProvider;
import java.nio.IntBuffer;

public final class DirectMultiDrawBatch extends MultiDrawBatch {
    public final long pElementPointer;
    public final long pElementCount;
    public final long pBaseVertex;

    private static final int POINTER_SHIFT = Integer.numberOfTrailingZeros(LWJGL.getPointerSize());

    public DirectMultiDrawBatch(int capacity) {
        super(capacity);

        int allocCount = this.capacity();

        this.pElementPointer = LWJGL.nmemAlignedAlloc(32, (long) allocCount * LWJGL.getPointerSize());
        if (this.pElementPointer == LWJGLServiceProvider.NULL) {
            throw new OutOfMemoryError("Failed to allocate element pointer array");
        }
        LWJGL.memSet(this.pElementPointer, 0x0, (long) allocCount * LWJGL.getPointerSize());

        this.pElementCount = LWJGL.nmemAlignedAlloc(32, (long) allocCount * Integer.BYTES);
        if (this.pElementCount == LWJGLServiceProvider.NULL) {
            LWJGL.nmemAlignedFree(this.pElementPointer);
            throw new OutOfMemoryError("Failed to allocate element count array");
        }
        this.pBaseVertex = LWJGL.nmemAlignedAlloc(32, (long) allocCount * Integer.BYTES);
        if (this.pBaseVertex == LWJGLServiceProvider.NULL) {
            LWJGL.nmemAlignedFree(this.pElementPointer);
            LWJGL.nmemAlignedFree(this.pElementCount);
            throw new OutOfMemoryError("Failed to allocate base vertex array");
        }
    }

    @Override
    public void delete() {
        LWJGL.nmemAlignedFree(this.pElementPointer);
        LWJGL.nmemAlignedFree(this.pElementCount);
        LWJGL.nmemAlignedFree(this.pBaseVertex);
    }

    @Override
    public void appendDrawCommand(int baseVertex, int elementCount, long elementPointer) {
        int index = this.size;

        LWJGL.memPutInt(this.pBaseVertex + ((long) index << 2), baseVertex);
        LWJGL.memPutInt(this.pElementCount + ((long) index << 2), elementCount);
        LWJGL.memPutAddress(this.pElementPointer + ((long) index << POINTER_SHIFT), elementPointer);

        // Element counts are never negative, so the sign bit of the negation is set iff the command is non-empty.
        this.size = index + ((-elementCount) >>> 31);

        // Safe to apply unconditionally: an empty command cannot raise the maximum.
        this.maxElementCount = Math.max(this.maxElementCount, elementCount);
    }

    @Override
    public void mergeIntoLastCommand(int additionalElementCount) {
        long countPointer = this.pElementCount + ((long) (this.size - 1) << 2);
        int mergedCount = LWJGL.memGetInt(countPointer) + additionalElementCount;

        LWJGL.memPutInt(countPointer, mergedCount);
        this.maxElementCount = Math.max(this.maxElementCount, mergedCount);
    }

    @Override
    public void upload(CommandList commandList) {
        // no-op: the GL call reads these arrays directly out of CPU memory
    }

    @Override
    public void execute(CommandList commandList, GlTessellation tessellation, GlPrimitiveType primitiveType) {
        try (DrawCommandList drawCommandList = commandList.beginTessellating(tessellation)) {
            drawCommandList.multiDrawElementsBaseVertex(this, primitiveType, GlIndexType.UNSIGNED_INT);
        }
    }
}
