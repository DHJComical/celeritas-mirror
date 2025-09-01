package org.embeddedt.embeddium.impl.render.chunk.region;

import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.*;
import org.embeddedt.embeddium.impl.gl.arena.PendingUpload;
import org.embeddedt.embeddium.impl.gl.arena.staging.FallbackStagingBuffer;
import org.embeddedt.embeddium.impl.gl.arena.staging.MappedStagingBuffer;
import org.embeddedt.embeddium.impl.gl.arena.staging.StagingBuffer;
import org.embeddedt.embeddium.impl.gl.attribute.GlVertexFormat;
import org.embeddedt.embeddium.impl.gl.device.CommandList;
import org.embeddedt.embeddium.impl.gl.device.RenderDevice;
import org.embeddedt.embeddium.impl.render.chunk.RenderPassConfiguration;
import org.embeddedt.embeddium.impl.render.chunk.RenderSection;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildOutput;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkSortOutput;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkTaskOutput;
import org.embeddedt.embeddium.impl.render.chunk.compile.executor.ChunkJobResult;
import org.embeddedt.embeddium.impl.render.chunk.data.BuiltSectionMeshParts;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class RenderRegionManager {
    private final Long2ReferenceOpenHashMap<RenderRegion> regions = new Long2ReferenceOpenHashMap<>();
    private final BitSet regionIds = new BitSet();
    private int nextFreeId = 0;

    private final StagingBuffer stagingBuffer;

    private final RenderPassConfiguration<?> renderPassConfiguration;

    public RenderRegionManager(CommandList commandList, RenderPassConfiguration<?> renderPassConfiguration) {
        this.stagingBuffer = createStagingBuffer(commandList);
        this.renderPassConfiguration = renderPassConfiguration;
    }

    public void update() {
        this.stagingBuffer.flip();

        try (CommandList commandList = RenderDevice.INSTANCE.createCommandList()) {
            Iterator<RenderRegion> it = this.regions.values()
                    .iterator();

            while (it.hasNext()) {
                RenderRegion region = it.next();
                region.update(commandList);

                if (region.isEmpty()) {
                    region.delete(commandList);

                    it.remove();

                    this.regionIds.clear(region.getId());
                    this.nextFreeId = Math.min(this.nextFreeId, region.getId());
                }
            }
        }
    }

    public void uploadMeshes(CommandList commandList, Collection<ChunkJobResult.Success<? extends ChunkTaskOutput>> results, Runnable graphUpdateTrigger) {
        for (var entry : this.createMeshUploadQueues(results)) {
            new MeshUploader(commandList, entry.getKey(), graphUpdateTrigger).processResults(entry.getValue());
        }
    }

    /* Copied from fastutil 8 as we don't have access to it when limited to fastutil 7 */
    private static <K, V> ObjectIterable<Reference2ReferenceMap.Entry<K, V>> fastIterable(Reference2ReferenceMap<K, V> map) {
        final ObjectSet<Reference2ReferenceMap.Entry<K, V>> entries = map.reference2ReferenceEntrySet();
        return entries instanceof Reference2ReferenceMap.FastEntrySet ? () -> ((Reference2ReferenceMap.FastEntrySet<K, V>)entries).fastIterator() : entries;
    }

    private class MeshUploader {
        private final Map<GlVertexFormat, ArrayList<PendingSectionUpload>> uploadsByFormat = new Object2ObjectOpenHashMap<>(2);
        private final CommandList commandList;
        private final RenderRegion region;
        private final Runnable graphUpdateTrigger;

        private boolean needIndexBuffer;

        private MeshUploader(CommandList commandList, RenderRegion region, Runnable graphUpdateTrigger) {
            this.commandList = commandList;
            this.region = region;
            this.graphUpdateTrigger = graphUpdateTrigger;
        }

        private ArrayList<PendingSectionUpload> getUploadQueue(TerrainRenderPass pass) {
            return uploadsByFormat.computeIfAbsent(pass.vertexType().getVertexFormat(), $ -> new ArrayList<>());
        }

        private void processBuildResult(ChunkBuildOutput result) {
            // Delete all existing data for the section in the region
            region.removeMeshes(result.render.getSectionIndex());

            // Add uploads for any new data
            for (var entry : fastIterable(result.meshes)) {
                BuiltSectionMeshParts mesh = Objects.requireNonNull(entry.getValue());

                needIndexBuffer |= mesh.indexBuffer() != null;

                getUploadQueue(entry.getKey()).add(new PendingMeshRebuildUpload(result.render, mesh, entry.getKey(),
                        PendingUpload.of(mesh.vertexBuffer()), PendingUpload.of(mesh.indexBuffer())));
            }
        }

        private void processSortResult(ChunkSortOutput result) {
            needIndexBuffer = true;

            for (var entry : fastIterable(result.meshes)) {
                var pass = entry.getKey();
                var mesh = entry.getValue();

                var storage = region.getStorage(pass);

                if (storage != null) {
                    storage.removeIndexBuffer(result.render.getSectionIndex());
                }

                getUploadQueue(entry.getKey()).add(new PendingMeshSortUpload(result.render, pass, PendingUpload.of(mesh.indexData())));
            }
        }

        public void processResults(Collection<? extends ChunkTaskOutput> results) {
            for (ChunkTaskOutput output : results) {
                if (output instanceof ChunkBuildOutput result) {
                    processBuildResult(result);
                } else if (output instanceof ChunkSortOutput result) {
                    processSortResult(result);
                } else {
                    throw new IllegalStateException("Unexpected result type: " + output.getClass().getName());
                }
            }

            // If we have nothing to upload, abort!
            if (uploadsByFormat.isEmpty()) {
                return;
            }

            boolean bufferChanged = false;

            for (var entry : uploadsByFormat.entrySet()) {
                var resources = region.createResources(entry.getKey(), commandList);
                var uploads = entry.getValue();
                var geometryArena = resources.getGeometryArena();

                bufferChanged |= geometryArena.upload(commandList, uploads.stream()
                        .map(PendingSectionUpload::vertexUpload).filter(Objects::nonNull));

                if (needIndexBuffer) {
                    bufferChanged |= resources.getOrCreateIndexArena(commandList).upload(commandList, uploads.stream()
                            .map(PendingSectionUpload::indexUpload).filter(Objects::nonNull));
                }
            }


            // If any of the buffers changed, the tessellation will need to be updated
            // Once invalidated the tessellation will be re-created on the next attempted use
            if (bufferChanged) {
                region.refresh(commandList);
            }

            int previousPassCookie = region.getPassSetUpdateCount();

            // Collect the upload results
            for (var uploads : uploadsByFormat.values()) {
                for (PendingSectionUpload upload : uploads) {
                    var storage = region.createStorage(upload.pass(), renderPassConfiguration);
                    if (upload instanceof PendingMeshRebuildUpload meshUpload) {
                        // Replace meshes
                        var indexResult = upload.indexUpload() != null ? upload.indexUpload().getResult() : null;
                        storage.setMeshes(upload.section().getSectionIndex(),
                                upload.vertexUpload().getResult(), indexResult, meshUpload.meshData().ranges());
                    } else if (upload instanceof PendingMeshSortUpload) {
                        // Replace index buffer
                        storage.replaceIndexBuffer(upload.section().getSectionIndex(), upload.indexUpload().getResult());
                    } else {
                        throw new IllegalStateException();
                    }
                }
            }

            region.removeEmptyStorages();

            if (region.getPassSetUpdateCount() != previousPassCookie) {
                graphUpdateTrigger.run();
            }
        }
    }

    private Reference2ReferenceMap.FastEntrySet<RenderRegion, List<ChunkTaskOutput>> createMeshUploadQueues(Collection<ChunkJobResult.Success<? extends ChunkTaskOutput>> results) {
        var map = new Reference2ReferenceOpenHashMap<RenderRegion, List<ChunkTaskOutput>>();

        for (var holder : results) {
            var result = holder.output();
            var queue = map.computeIfAbsent(result.render.getRegion(), k -> new ArrayList<>());
            queue.add(result);
        }

        return map.reference2ReferenceEntrySet();
    }

    public void delete(CommandList commandList) {
        for (RenderRegion region : this.regions.values()) {
            region.delete(commandList);
        }

        this.regions.clear();
        this.stagingBuffer.delete(commandList);
    }

    public Collection<RenderRegion> getLoadedRegions() {
        return this.regions.values();
    }

    public StagingBuffer getStagingBuffer() {
        return this.stagingBuffer;
    }

    public RenderRegion createForChunk(int chunkX, int chunkY, int chunkZ) {
        return this.create(chunkX >> RenderRegion.REGION_WIDTH_SH,
                chunkY >> RenderRegion.REGION_HEIGHT_SH,
                chunkZ >> RenderRegion.REGION_LENGTH_SH);
    }

    private int getNextId() {
        int id = this.nextFreeId;
        this.nextFreeId = this.regionIds.nextClearBit(id + 1);
        this.regionIds.set(id);
        return id;
    }

    @NotNull
    private RenderRegion create(int x, int y, int z) {
        var key = RenderRegion.key(x, y, z);
        var instance = this.regions.get(key);

        if (instance == null) {
            this.regions.put(key, instance = new RenderRegion(x, y, z, this.getNextId(), this.stagingBuffer));
        }

        return instance;
    }

    public int getRegionIdsLength() {
        return this.regionIds.length();
    }

    private interface PendingSectionUpload {
        RenderSection section();
        TerrainRenderPass pass();
        PendingUpload vertexUpload();
        PendingUpload indexUpload();
    }

    private record PendingMeshRebuildUpload(RenderSection section, BuiltSectionMeshParts meshData, TerrainRenderPass pass,
                                            PendingUpload vertexUpload, PendingUpload indexUpload) implements PendingSectionUpload {}

    private record PendingMeshSortUpload(RenderSection section, TerrainRenderPass pass, PendingUpload indexUpload) implements PendingSectionUpload {
        @Override
        public PendingUpload vertexUpload() {
            return null;
        }
    }


    private static StagingBuffer createStagingBuffer(CommandList commandList) {
        if (MappedStagingBuffer.isSupported(RenderDevice.INSTANCE)) {
            return new MappedStagingBuffer(commandList);
        }

        return new FallbackStagingBuffer(commandList);
    }
}
