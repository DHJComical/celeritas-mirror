package org.embeddedt.embeddium.impl.render.chunk.data;

import org.embeddedt.embeddium.impl.gl.arena.GlBufferSegment;
import org.embeddedt.embeddium.impl.gl.util.VertexRange;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.embeddedt.embeddium.impl.render.chunk.compile.sorting.ChunkPrimitiveType;
import org.embeddedt.embeddium.impl.render.chunk.multidraw.CachedBatch;
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

    public record BatchCacheParams(boolean useBlockFaceCulling) {}

    // we only need to cache two batches in practice (shadow pass for shaders disables block face culling), so we unroll
    // what would otherwise be an LRU for simplicity
    private BatchCacheParams firstBatchParams;
    private CachedBatch firstCachedBatch;
    private BatchCacheParams secondBatchParams;
    private CachedBatch secondCachedBatch;

    public SectionRenderDataStorage(ChunkPrimitiveType primitiveType, boolean sorted) {
        this.storageStrategy = sorted ? SectionRenderDataUnsafe.Strategy.FULL : SectionRenderDataUnsafe.Strategy.COMPACT;
        this.pMeshDataArray = this.storageStrategy.allocateHeap();
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

        int vertexOffset = allocation.getOffset();
        int indexOffset = indexAllocation != null ? indexAllocation.getOffset() * 4 : 0;

        this.storageStrategy.writeMeshesAndSliceMask(this.pMeshDataArray, localSectionIndex, vertexOffset, indexOffset, ranges, this.primitiveType);

        this.invalidateCachedBatches();
    }

    public void removeMeshes(int localSectionIndex) {
        if (this.allocations[localSectionIndex] != null) {
            this.allocations[localSectionIndex].delete();
            this.allocations[localSectionIndex] = null;

            this.storageStrategy.clearRow(this.pMeshDataArray, localSectionIndex);

            this.numAllocations--;

            this.invalidateCachedBatches();
        }

        removeIndexBuffer(localSectionIndex);
    }

    public void removeIndexBuffer(int localSectionIndex) {
        if (this.indexAllocations[localSectionIndex] != null) {
            this.indexAllocations[localSectionIndex].delete();
            this.indexAllocations[localSectionIndex] = null;

            this.invalidateCachedBatches();
        }
    }

    public void replaceIndexBuffer(int localSectionIndex, GlBufferSegment indexAllocation) {
        removeIndexBuffer(localSectionIndex);

        this.indexAllocations[localSectionIndex] = indexAllocation;

        var pMeshData = this.getDataPointer(localSectionIndex);

        int indexOffset = indexAllocation != null ? indexAllocation.getOffset() * 4 : 0;

        this.storageStrategy.writeIndexOffsets(pMeshData, indexOffset, this.primitiveType);

        this.invalidateCachedBatches();
    }

    public void onBufferResized() {
        for (int sectionIndex = 0; sectionIndex < RenderRegion.REGION_SIZE; sectionIndex++) {
            this.updateMeshes(sectionIndex);
        }

        this.invalidateCachedBatches();
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
    public long getRowBasePointer() {
        return this.storageStrategy.getRowBasePointer(this.pMeshDataArray);
    }

    public int getSliceMask(int sectionIndex) {
        return this.storageStrategy.getSliceMask(this.pMeshDataArray, sectionIndex);
    }

    public @Nullable CachedBatch getCachedMultiDrawBatch(BatchCacheParams params) {
        if (params.equals(firstBatchParams)) {
            return firstCachedBatch;
        } else if (params.equals(secondBatchParams)) {
            return secondCachedBatch;
        } else {
            return null;
        }
    }

    public void storeCachedMultiDrawBatch(BatchCacheParams params, CachedBatch batch) {
        if (params.equals(firstBatchParams) || firstBatchParams == null) {
            if (firstCachedBatch != null) {
                firstCachedBatch.delete();
            }
            firstBatchParams = params;
            firstCachedBatch = batch;
        } else {
            if (secondCachedBatch != null) {
                secondCachedBatch.delete();
            }
            secondBatchParams = params;
            secondCachedBatch = batch;
        }
    }

    private void invalidateCachedBatches() {
        if (firstCachedBatch != null) {
            firstCachedBatch.delete();
            firstCachedBatch = null;
        }
        if (secondCachedBatch != null) {
            secondCachedBatch.delete();
            secondCachedBatch = null;
        }
        firstBatchParams = null;
        secondBatchParams = null;
    }

    public void delete() {
        this.invalidateCachedBatches();

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
