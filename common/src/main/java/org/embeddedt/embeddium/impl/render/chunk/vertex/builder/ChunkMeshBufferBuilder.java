package org.embeddedt.embeddium.impl.render.chunk.vertex.builder;

import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexEncoder;
import org.embeddedt.embeddium.impl.render.chunk.sorting.SortState;
import org.embeddedt.embeddium.impl.render.chunk.sorting.TranslucentQuadRecorder;
import org.jetbrains.annotations.Nullable;
import static org.taumc.celeritas.lwjgl.LWJGLServiceProvider.LWJGL;
import java.nio.ByteBuffer;

public class ChunkMeshBufferBuilder {
    private final ChunkVertexEncoder encoder;
    private final int bytesPerPrimitive;
    private final int verticesPerPrimitive;

    private final int initialCapacity;
    private final TranslucentQuadRecorder analyzer;

    private ByteBuffer buffer;
    private int count;
    private int capacity;
    private int sectionIndex;

    public ChunkMeshBufferBuilder(ChunkVertexEncoder encoder, int bytesPerPrimitive, int verticesPerPrimitive, int initialCapacity, boolean collectSortState) {
        this.encoder = encoder;
        this.bytesPerPrimitive = bytesPerPrimitive;
        this.verticesPerPrimitive = verticesPerPrimitive;

        this.buffer = null;

        this.capacity = 0;
        this.initialCapacity = initialCapacity;

        this.analyzer = collectSortState ? new TranslucentQuadRecorder() : null;
    }

    public void push(ChunkVertexEncoder.Vertex[] vertices, Material material) {
        if (vertices.length != this.verticesPerPrimitive) {
            throw new IllegalArgumentException("Expected one primitive of " + this.verticesPerPrimitive + " vertices, got " + vertices.length);
        }

        var primitiveStart = this.byteSize();

        if (primitiveStart + this.bytesPerPrimitive >= this.capacity) {
            this.grow(this.bytesPerPrimitive);
        }

        long ptr = LWJGL.memAddress(this.buffer, primitiveStart);

        if (this.analyzer != null) {
            for (ChunkVertexEncoder.Vertex vertex : vertices) {
                this.analyzer.capture(vertex);
            }
        }

        this.encoder.writePrimitive(ptr, material, vertices, this.sectionIndex);

        this.count += vertices.length;
    }

    private void grow(int bytesNeeded) {
        // Grow by a factor of 2, or by however many bytes more we need, whichever is larger.
        int newCapacity = Math.max(this.capacity * 2, this.capacity + bytesNeeded);
        // Ensure we allocate at least initialCapacity bytes
        newCapacity = Math.max(newCapacity, this.initialCapacity);

        this.buffer = LWJGL.memRealloc(this.buffer, newCapacity);
        this.capacity = newCapacity;
    }

    public void start(int sectionIndex) {
        this.count = 0;
        this.sectionIndex = sectionIndex;
        if(this.analyzer != null) {
            this.analyzer.clear();
        }
    }

    @Nullable
    public SortState getSortState() {
        return this.analyzer != null ? this.analyzer.getSortState() : null;
    }

    public void resetSortState() {
        if (this.analyzer != null) {
            this.analyzer.clear();
        }
    }

    public void destroy() {
        if (this.buffer != null) {
            LWJGL.memFree(this.buffer);
        }

        this.buffer = null;
        this.capacity = 0;

        this.resetSortState();
    }

    public boolean isEmpty() {
        return this.count == 0;
    }

    public ByteBuffer slice() {
        if (this.isEmpty()) {
            throw new IllegalStateException("No vertex data in buffer");
        }

        return LWJGL.memSlice(this.buffer, 0, this.byteSize());
    }

    public int count() {
        return this.count;
    }

    private int byteSize() {
        return (this.count / this.verticesPerPrimitive) * this.bytesPerPrimitive;
    }
}
