package org.taumc.celeritas.render.terrain;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.embeddedt.embeddium.impl.gl.device.CommandList;
import org.embeddedt.embeddium.impl.render.chunk.*;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildOutput;
import org.embeddedt.embeddium.impl.render.chunk.compile.sorting.QuadPrimitiveType;
import org.embeddedt.embeddium.impl.render.chunk.compile.tasks.ChunkBuilderTask;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.AsyncOcclusionMode;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.parameters.AlphaCutoffParameter;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkMeshFormats;
import org.embeddedt.embeddium.impl.render.viewport.Viewport;
import org.jetbrains.annotations.Nullable;
import org.taumc.celeritas.render.terrain.task.MeshingTask;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class PostmodernRenderSectionManager extends RenderSectionManager {
    private final ClientLevel world;

    public PostmodernRenderSectionManager(RenderPassConfiguration<ChunkSectionLayer> config, ClientLevel level, int renderDistance, CommandList commandList) {
        super(config, () -> new PostmodernChunkBuildContext(config), PostmodernChunkRenderer::new, renderDistance, commandList,
                level.getMinSectionY(), level.getMaxSectionY() + 1, 0, false);
        this.world = level;
    }

    private static Optional<Float> getAlphaCutoff(ChunkSectionLayer layer) {
        return Optional.ofNullable(layer.pipeline().getShaderDefines().values().get("ALPHA_CUTOUT")).map(Float::parseFloat);
    }

    private record VanillaTerrainConfig(boolean hasAlphaCutoff, boolean translucencySorted) {
        private static VanillaTerrainConfig fromLayer(ChunkSectionLayer layer) {
            boolean hasAlphaCutoff = getAlphaCutoff(layer).orElse(0f) > 0;
            return new VanillaTerrainConfig(hasAlphaCutoff, layer.sortOnUpload());
        }
    }

    private record PostmodernPipeline(VanillaTerrainConfig config) implements TerrainRenderPass.PipelineState {
        @Override
        public void setup() {

        }

        @Override
        public void clear() {

        }
    }

    private static RenderPassConfiguration<ChunkSectionLayer> createRenderPassConfiguration() {
        Map<ChunkSectionLayer, Material> materialMap = new EnumMap<>(ChunkSectionLayer.class);
        Map<ChunkSectionLayer, Collection<TerrainRenderPass>> renderPasses = new EnumMap<>(ChunkSectionLayer.class);
        Map<VanillaTerrainConfig, TerrainRenderPass> vanillaPasses = new HashMap<>();
        AtomicInteger id = new AtomicInteger(1);
        for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
            var config = VanillaTerrainConfig.fromLayer(layer);
            var terrainPass = vanillaPasses.computeIfAbsent(config, c -> {
                var pass = TerrainRenderPass.builder()
                        .name("generated_" + id.getAndIncrement())
                        .fragmentDiscard(c.hasAlphaCutoff())
                        .vertexType(ChunkMeshFormats.VANILLA_LIKE)
                        .primitiveType(QuadPrimitiveType.TRIANGULATED)
                        .useReverseOrder(c.translucencySorted())
                        .pipelineState(new PostmodernPipeline(c))
                        .useTranslucencySorting(c.translucencySorted())
                        .build();
                renderPasses.computeIfAbsent(layer, $ -> new ArrayList<>()).add(pass);
                return pass;
            });
            var alphaCutoff = getAlphaCutoff(layer).map(a -> {
                if (Mth.equal(a, 0.01f)) {
                    return AlphaCutoffParameter.ONE_TENTH; // TODO figure out how to deal with 0.01
                } else {
                    return AlphaCutoffParameter.valueOf(a);
                }
            }).orElse(AlphaCutoffParameter.ZERO);
            materialMap.put(layer, new Material(terrainPass, alphaCutoff, false));
        }
        renderPasses.replaceAll((layer, list) -> List.copyOf(list));
        return new RenderPassConfiguration<>(materialMap, renderPasses,
                materialMap.get(ChunkSectionLayer.SOLID),
                materialMap.get(ChunkSectionLayer.CUTOUT),
                materialMap.get(ChunkSectionLayer.TRANSLUCENT));
    }

    public static PostmodernRenderSectionManager create(ClientLevel world, int renderDistance, CommandList commandList) {
        var config = createRenderPassConfiguration();
        return new PostmodernRenderSectionManager(config, world, renderDistance, commandList);
    }

    @Override
    protected AsyncOcclusionMode getAsyncOcclusionMode() {
        return AsyncOcclusionMode.EVERYTHING;
    }

    @Override
    protected boolean useFogOcclusion() {
        return false;
    }

    @Override
    protected boolean shouldUseOcclusionCulling(Viewport viewport, boolean spectator) {
        final boolean useOcclusionCulling;
        var camBlockPos = viewport.getBlockCoord();
        BlockPos origin = new BlockPos(camBlockPos.x(), camBlockPos.y(), camBlockPos.z());

        if (spectator && this.world.getBlockState(origin).isSolidRender())
        {
            useOcclusionCulling = false;
        } else {
            useOcclusionCulling = Minecraft.getInstance().smartCull;
        }
        return useOcclusionCulling;
    }

    @Override
    protected boolean isSectionVisuallyEmpty(int x, int y, int z) {
        ChunkAccess chunk = this.world.getChunk(x, z);
        LevelChunkSection section = chunk.getSections()[this.world.getSectionIndexFromSectionY(y)];

        return section == null || section.hasOnlyAir();
    }

    @Override
    protected @Nullable ChunkBuilderTask<ChunkBuildOutput> createRebuildTask(RenderSection render, int frame) {
        if (isSectionVisuallyEmpty(render.getChunkX(), render.getChunkY(), render.getChunkZ())) {
            return null;
        }

        // TODO: port faster & safer chunk section cloning
        RenderRegionCache renderregioncache = new RenderRegionCache();
        var region = renderregioncache.createRegion(this.world, SectionPos.asLong(render.getChunkX(), render.getChunkY(), render.getChunkZ()));

        return new MeshingTask(render, region, cameraPosition, frame);
    }
}
