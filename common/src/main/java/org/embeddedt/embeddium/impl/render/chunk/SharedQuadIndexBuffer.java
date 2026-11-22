package org.embeddedt.embeddium.impl.render.chunk;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import org.embeddedt.embeddium.impl.gl.buffer.GlBuffer;
import org.embeddedt.embeddium.impl.gl.buffer.GlBufferMapFlags;
import org.embeddedt.embeddium.impl.gl.buffer.GlBufferUsage;
import org.embeddedt.embeddium.impl.gl.buffer.GlMutableBuffer;
import org.embeddedt.embeddium.impl.gl.device.CommandList;
import org.embeddedt.embeddium.impl.gl.util.EnumBitField;
import org.embeddedt.embeddium.impl.render.chunk.compile.sorting.ChunkPrimitiveType;

/**
 * An index buffer of sequential quads, shared by everything that draws with the same primitive type. Users hold a
 * {@link Handle} from {@link #acquire}. The buffer is sized for the largest request among the current handles, so it
 * shrinks when a large user releases its handle, and it is deleted when the last handle is released.
 * <p>
 * Resizing reallocates the same buffer object, so vertex arrays that reference it stay valid.
 */
public final class SharedQuadIndexBuffer {
    private static final int HEADROOM_PRIMITIVES = 16384;

    private static final Reference2ReferenceMap<ChunkPrimitiveType, SharedQuadIndexBuffer> BUFFERS = new Reference2ReferenceOpenHashMap<>();

    private final GlMutableBuffer buffer;
    private final ChunkPrimitiveType primitiveType;
    private final ReferenceArrayList<Handle> handles = new ReferenceArrayList<>();

    private int maxPrimitives;

    private SharedQuadIndexBuffer(CommandList commandList, ChunkPrimitiveType primitiveType) {
        this.buffer = commandList.createMutableBuffer();
        this.primitiveType = primitiveType;
    }

    /**
     * {@return a new handle to the shared buffer for the given primitive type} The caller must
     * {@linkplain Handle#release release} it when it no longer draws with the buffer.
     */
    public static Handle acquire(CommandList commandList, ChunkPrimitiveType primitiveType) {
        SharedQuadIndexBuffer shared = BUFFERS.get(primitiveType);

        if (shared == null) {
            shared = new SharedQuadIndexBuffer(commandList, primitiveType);
            BUFFERS.put(primitiveType, shared);
        }

        Handle handle = shared.new Handle();
        shared.handles.add(handle);

        return handle;
    }

    private void ensureCapacity(CommandList commandList, int primitiveCount) {
        if (primitiveCount > this.maxPrimitives) {
            this.resize(commandList, Math.max(this.maxPrimitives * 2, primitiveCount + HEADROOM_PRIMITIVES));
        }
    }

    private void release(CommandList commandList, Handle handle) {
        this.handles.remove(handle);

        if (this.handles.isEmpty()) {
            commandList.deleteBuffer(this.buffer);
            BUFFERS.remove(this.primitiveType);
            return;
        }

        int required = 0;

        for (Handle remaining : this.handles) {
            required = Math.max(required, remaining.requiredPrimitives);
        }

        int target = required + HEADROOM_PRIMITIVES;

        // Only shrink when it frees a good amount, so users near the boundary don't reallocate over and over
        if (this.maxPrimitives > target * 2) {
            this.resize(commandList, target);
        }
    }

    private void resize(CommandList commandList, int primitiveCount) {
        var bufferSize = this.primitiveType.getIndexBufferSize(primitiveCount);

        commandList.allocateStorage(this.buffer, bufferSize, GlBufferUsage.STATIC_DRAW);

        var mapped = commandList.mapBuffer(this.buffer, 0, bufferSize, EnumBitField.of(GlBufferMapFlags.INVALIDATE_BUFFER, GlBufferMapFlags.WRITE, GlBufferMapFlags.UNSYNCHRONIZED));
        this.primitiveType.generateSimpleIndexBuffer(mapped.getMemoryBuffer(), primitiveCount);

        commandList.unmap(mapped);

        this.maxPrimitives = primitiveCount;
    }

    /**
     * One user's reference to a shared index buffer.
     */
    public final class Handle {
        private int requiredPrimitives;
        private boolean released;

        private Handle() {
        }

        /**
         * Makes sure the buffer holds at least the given number of indices. The largest count a handle has asked for
         * is kept until it is released.
         */
        public void ensureCapacity(CommandList commandList, int elementCount) {
            this.checkNotReleased();

            int primitiveCount = elementCount / SharedQuadIndexBuffer.this.primitiveType.getIndexBufferElementsPerPrimitive();
            this.requiredPrimitives = Math.max(this.requiredPrimitives, primitiveCount);

            SharedQuadIndexBuffer.this.ensureCapacity(commandList, primitiveCount);
        }

        public GlBuffer getBufferObject() {
            this.checkNotReleased();

            return SharedQuadIndexBuffer.this.buffer;
        }

        public void release(CommandList commandList) {
            this.checkNotReleased();
            this.released = true;

            SharedQuadIndexBuffer.this.release(commandList, this);
        }

        private void checkNotReleased() {
            if (this.released) {
                throw new IllegalStateException("Index buffer handle was already released");
            }
        }
    }
}
