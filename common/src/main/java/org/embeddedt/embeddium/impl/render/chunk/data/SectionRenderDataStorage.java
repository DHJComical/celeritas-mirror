package org.embeddedt.embeddium.impl.render.chunk.data;

import org.embeddedt.embeddium.impl.gl.arena.GlBufferSegment;
import org.embeddedt.embeddium.impl.gl.util.VertexRange;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.embeddedt.embeddium.impl.render.chunk.compile.sorting.ChunkPrimitiveType;
import org.embeddedt.embeddium.impl.render.chunk.region.RenderRegion;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;

public class SectionRenderDataStorage {
    private final GlBufferSegment[] allocations = new GlBufferSegment[RenderRegion.REGION_SIZE];
    private final GlBufferSegment[] indexAllocations = new GlBufferSegment[RenderRegion.REGION_SIZE];

    private final long pMeshDataArray;
    private final ChunkPrimitiveType primitiveType;
    private final SectionRenderDataUnsafe.Strategy storageStrategy;

    private int numAllocations;

    public SectionRenderDataStorage(ChunkPrimitiveType primitiveType, boolean sorted) {
        this.storageStrategy = sorted ? SectionRenderDataUnsafe.Strategy.FULL : SectionRenderDataUnsafe.Strategy.COMPACT;
        this.pMeshDataArray = this.storageStrategy.allocateHeap(RenderRegion.REGION_SIZE);
        if (this.pMeshDataArray == 0) {
            throw new OutOfMemoryError("Failed to allocate mesh data array");
        }
        this.primitiveType = primitiveType;
    }

    public boolean isEmpty() {
        return this.numAllocations == 0;
    }

    public ChunkPrimitiveType getPrimitiveType() {
        return this.primitiveType;
    }

    public void setMeshes(int localSectionIndex,
                          GlBufferSegment allocation, @Nullable GlBufferSegment indexAllocation, Map<ModelQuadFacing, VertexRange> ranges) {
        if (this.allocations[localSectionIndex] != null) {
            this.allocations[localSectionIndex].delete();
            this.allocations[localSectionIndex] = null;
            this.numAllocations--;
        }

        if (this.indexAllocations[localSectionIndex] != null) {
            this.indexAllocations[localSectionIndex].delete();
            this.indexAllocations[localSectionIndex] = null;
        }

        this.allocations[localSectionIndex] = allocation;
        this.indexAllocations[localSectionIndex] = indexAllocation;
        this.numAllocations++;

        var pMeshData = this.getDataPointer(localSectionIndex);

        int vertexOffset = allocation.getOffset();
        int indexOffset = indexAllocation != null ? indexAllocation.getOffset() * 4 : 0;

        int sliceMask = this.storageStrategy.writeMeshes(pMeshData, vertexOffset, indexOffset, ranges, this.primitiveType);
        this.storageStrategy.setSliceMask(this.pMeshDataArray, localSectionIndex, sliceMask);
    }

    public void removeMeshes(int localSectionIndex) {
        if (this.allocations[localSectionIndex] != null) {
            this.allocations[localSectionIndex].delete();
            this.allocations[localSectionIndex] = null;

            this.storageStrategy.setSliceMask(this.pMeshDataArray, localSectionIndex, 0);
            this.storageStrategy.clear(this.getDataPointer(localSectionIndex));

            this.numAllocations--;
        }

        removeIndexBuffer(localSectionIndex);
    }

    public void removeIndexBuffer(int localSectionIndex) {
        if (this.indexAllocations[localSectionIndex] != null) {
            this.indexAllocations[localSectionIndex].delete();
            this.indexAllocations[localSectionIndex] = null;
        }
    }

    public void replaceIndexBuffer(int localSectionIndex, GlBufferSegment indexAllocation) {
        removeIndexBuffer(localSectionIndex);

        this.indexAllocations[localSectionIndex] = indexAllocation;

        var pMeshData = this.getDataPointer(localSectionIndex);

        int indexOffset = indexAllocation != null ? indexAllocation.getOffset() * 4 : 0;

        this.storageStrategy.writeIndexOffsets(pMeshData, indexOffset, this.primitiveType);
    }

    public void onBufferResized() {
        for (int sectionIndex = 0; sectionIndex < RenderRegion.REGION_SIZE; sectionIndex++) {
            this.updateMeshes(sectionIndex);
        }
    }

    private void updateMeshes(int sectionIndex) {
        var allocation = this.allocations[sectionIndex];

        if (allocation == null) {
            return;
        }

        var indexAllocation = this.indexAllocations[sectionIndex];

        var vertexOffset = allocation.getOffset();
        var indexOffset = indexAllocation != null ? indexAllocation.getOffset() * 4 : 0;

        var data = this.getDataPointer(sectionIndex);

        this.storageStrategy.rebase(data, vertexOffset, indexOffset, this.primitiveType);
    }

    public long getDataPointer(int sectionIndex) {
        return this.storageStrategy.heapPointer(this.pMeshDataArray, sectionIndex);
    }

    /**
     * {@return the base address of the mesh data array}
     * <p>
     * Row {@code i} begins {@code i * stride} bytes in. Exposed for hot loops which already know their layout
     * statically, so that they can hoist the base and stride and index the array themselves rather than dispatching
     * through {@link #getDataPointer} once per section.
     */
    public long getBasePointer() {
        return this.storageStrategy.getBasePointer(this.pMeshDataArray);
    }

    public int getSliceMask(int sectionIndex) {
        return this.storageStrategy.getSliceMask(this.pMeshDataArray, sectionIndex);
    }

    public void delete() {
        for (var allocation : this.allocations) {
            if (allocation != null) {
                allocation.delete();
            }
        }

        for (var allocation : this.indexAllocations) {
            if (allocation != null) {
                allocation.delete();
            }
        }

        Arrays.fill(this.allocations, null);
        Arrays.fill(this.indexAllocations, null);

        this.storageStrategy.freeHeap(this.pMeshDataArray);

        this.numAllocations = 0;
    }
}
