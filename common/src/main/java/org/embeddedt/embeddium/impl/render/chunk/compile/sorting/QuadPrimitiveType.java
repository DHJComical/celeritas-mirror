package org.embeddedt.embeddium.impl.render.chunk.compile.sorting;

import org.embeddedt.embeddium.impl.render.chunk.sorting.PartitionTree;
import org.embeddedt.embeddium.impl.render.chunk.sorting.SortState;
import org.embeddedt.embeddium.impl.render.chunk.sorting.TranslucentSorter;
import org.jetbrains.annotations.Nullable;
import static org.taumc.celeritas.lwjgl.LWJGLServiceProvider.LWJGL;

import java.nio.ByteBuffer;

public final class QuadPrimitiveType implements ChunkPrimitiveType {
    /**
     * Generates index buffers that decompose quads into two triangles (suitable for core profile rendering).
     */
    public static final QuadPrimitiveType TRIANGULATED = new QuadPrimitiveType(true);
    /**
     * Generates index buffers suited for working with quad(-like) primitives directly.
     */
    public static final QuadPrimitiveType DIRECT = new QuadPrimitiveType(false);

    private final boolean triangulating;

    private static final int VERTICES_PER_PRIMITIVE = 4;

    public QuadPrimitiveType(boolean triangulating) {
        this.triangulating = triangulating;
    }

    @Override
    public int getIndexBufferElementsPerPrimitive() {
        return triangulating ? 6 : 4;
    }

    @Override
    public int getVerticesPerPrimitive() {
        return VERTICES_PER_PRIMITIVE;
    }

    @Override
    public void generateSimpleIndexBuffer(ByteBuffer indexBuffer, int numPrimitives) {
        int minimumRequiredBufferSize = getIndexBufferSize(numPrimitives);
        if(indexBuffer.capacity() < minimumRequiredBufferSize) {
            throw new IllegalStateException("Given index buffer has length " + indexBuffer.capacity() + " but we need " + minimumRequiredBufferSize);
        }
        long ptr = LWJGL.memAddress(indexBuffer);

        for (int primitiveIndex = 0; primitiveIndex < numPrimitives; primitiveIndex++) {
            this.writePrimitive(ptr, primitiveIndex, primitiveIndex);
        }
    }

    /** writes the indices of the given quad as the primitive at the given position */
    private void writePrimitive(long ptr, int primitiveIndex, int quad) {
        int indexOffset = primitiveIndex * this.getIndexBufferElementsPerPrimitive();
        int vertexOffset = quad * VERTICES_PER_PRIMITIVE;

        LWJGL.memPutInt(ptr + (indexOffset + 0) * 4, vertexOffset + 0);
        LWJGL.memPutInt(ptr + (indexOffset + 1) * 4, vertexOffset + 1);
        LWJGL.memPutInt(ptr + (indexOffset + 2) * 4, vertexOffset + 2);

        if (this.triangulating) {
            LWJGL.memPutInt(ptr + (indexOffset + 3) * 4, vertexOffset + 2);
            LWJGL.memPutInt(ptr + (indexOffset + 4) * 4, vertexOffset + 3);
            LWJGL.memPutInt(ptr + (indexOffset + 5) * 4, vertexOffset + 0);
        } else {
            LWJGL.memPutInt(ptr + (indexOffset + 3) * 4, vertexOffset + 3);
        }
    }

    private void generateIndexBuffer(ByteBuffer indexBuffer, int[] primitiveMapping) {
        int bufferSize = getIndexBufferSize(primitiveMapping.length);
        if(indexBuffer.capacity() != bufferSize) {
            throw new IllegalStateException("Given index buffer has length " + indexBuffer.capacity() + " but we expected " + bufferSize);
        }
        long ptr = LWJGL.memAddress(indexBuffer);

        for (int primitiveIndex = 0; primitiveIndex < primitiveMapping.length; primitiveIndex++) {
            this.writePrimitive(ptr, primitiveIndex, primitiveMapping[primitiveIndex]);
        }
    }

    /** streams a partition tree's readout straight into the index buffer */
    private final class BufferSink implements PartitionTree.Sink {
        private final long ptr;
        private int primitiveIndex;

        private BufferSink(long ptr) {
            this.ptr = ptr;
        }

        @Override
        public void accept(int[] quads) {
            for (int quad : quads) {
                writePrimitive(this.ptr, this.primitiveIndex++, quad);
            }
        }
    }

    @Override
    public void generateSortedIndexBuffer(ByteBuffer indexBuffer, int quadCount, @Nullable SortState chunkData, float x, float y, float z) {
        if (chunkData instanceof PartitionTree tree) {
            int bufferSize = getIndexBufferSize(quadCount);
            if (indexBuffer.capacity() != bufferSize) {
                throw new IllegalStateException("Given index buffer has length " + indexBuffer.capacity() + " but we expected " + bufferSize);
            }
            if (quadCount != tree.quadCount()) {
                throw new IllegalStateException(String.format("Mismatched SortState (%d) vs given quad count (%d)", tree.quadCount(), quadCount));
            }

            var sink = new BufferSink(LWJGL.memAddress(indexBuffer));
            tree.order(x, y, z, sink);

            if (sink.primitiveIndex != quadCount) {
                throw new IllegalStateException(String.format("Partition tree wrote %d of %d quads", sink.primitiveIndex, quadCount));
            }
            return;
        }

        int[] order = chunkData != null ? TranslucentSorter.order(chunkData, x, y, z) : null;

        if (order == null) {
            generateSimpleIndexBuffer(indexBuffer, quadCount);
            return;
        }

        if (quadCount != order.length) {
            throw new IllegalStateException(String.format("Mismatched SortState (%d) vs given quad count (%d)", order.length, quadCount));
        }

        generateIndexBuffer(indexBuffer, order);
    }
}
