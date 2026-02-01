package org.taumc.celeritas.impl.render.terrain;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.embeddedt.embeddium.impl.gl.device.CommandList;
import org.embeddedt.embeddium.impl.gl.device.RenderDevice;
import org.embeddedt.embeddium.impl.render.chunk.*;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildOutput;
import org.embeddedt.embeddium.impl.render.chunk.compile.tasks.ChunkBuilderTask;
import org.embeddedt.embeddium.impl.render.chunk.data.BuiltRenderSectionData;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.AsyncOcclusionMode;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderInterface;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderTextureSlot;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexType;
import org.embeddedt.embeddium.impl.render.viewport.Viewport;
import org.embeddedt.embeddium.impl.util.position.SectionPos;
import org.jetbrains.annotations.Nullable;
import org.taumc.celeritas.impl.render.terrain.compile.PrimitiveBuiltRenderSectionData;
import org.taumc.celeritas.impl.render.terrain.compile.PrimitiveChunkBuildContext;
import org.taumc.celeritas.impl.render.terrain.compile.task.ChunkBuilderMeshingTask;
import org.taumc.celeritas.impl.world.cloned.ChunkRenderContext;

import java.util.Collection;

public class PrimitiveRenderSectionManager extends RenderSectionManager {
    private final World world;
    private final ReferenceOpenHashSet<RenderSection> sectionsWithSkyLight = new ReferenceOpenHashSet<>();

    public PrimitiveRenderSectionManager(RenderPassConfiguration<?> configuration, World world, int renderDistance, CommandList commandList, int minSection, int maxSection, int requestedThreads) {
        super(configuration, () -> new PrimitiveChunkBuildContext(world, configuration), ChunkRenderer::new, renderDistance, commandList, minSection, maxSection, requestedThreads);
        this.world = world;
    }

    public static PrimitiveRenderSectionManager create(ChunkVertexType vertexType, World world, int renderDistance, CommandList commandList) {
        // TODO support thread option
        //? if <1.8 {
        int idealThreadCount = -1;
        //?} else
        /*int idealThreadCount = 0;*/
        //? if <1.2 {
        /*int maxSection = 8;
        *///?} else
        int maxSection = world.getHeight() / 16;
        return new PrimitiveRenderSectionManager(PrimitiveRenderPassConfigurationBuilder.build(vertexType), world, renderDistance, commandList,
                0, maxSection,
                idealThreadCount);
    }

    @Override
    protected AsyncOcclusionMode getAsyncOcclusionMode() {
        return AsyncOcclusionMode.EVERYTHING;
    }

    @Override
    protected boolean shouldRespectUpdateTaskQueueSizeLimit() {
        return true;
    }

    @Override
    protected boolean useFogOcclusion() {
        return true;
    }

    @Override
    protected boolean shouldUseOcclusionCulling(Viewport positionedViewport, boolean spectator) {
        final boolean useOcclusionCulling;
        var camBlockPos = positionedViewport.getBlockCoord();

        //? if <1.8 {
        var block = this.world.getBlock(camBlockPos.x(), camBlockPos.y(), camBlockPos.z());
        //?} else
        /*var block = this.world.getBlockState(new net.minecraft.util.math.BlockPos(camBlockPos.x(), camBlockPos.y(), camBlockPos.z())).getBlock();*/

        //? if >=1.7 {
        /*boolean opaque = block.isOpaqueCube();
        *///?} else
        boolean opaque = Block.IS_OPAQUE[block];

        useOcclusionCulling = !spectator || !opaque;

        return useOcclusionCulling;
    }

    @Override
    protected boolean isSectionVisuallyEmpty(int x, int y, int z) {
        var chunk = this.world.getChunkAt(x, z);
        return chunk.isEmpty();
    }

    //? if >=1.8 {
    /*private void populateTileEntities(WorldChunk chunk, int sectionY) {
        if (chunk.isEmpty()) {
            return;
        }
        var sections = chunk.getSections();
        if (sectionY < 0 || sectionY > sections.length) {
            return;
        }

        var section = sections[sectionY];

        if (section == null || section.isEmpty()) {
            return;
        }

        int sectionBlockY = sectionY * 16;
        int chunkBlockX = chunk.chunkX * 16;
        int chunkBlockZ = chunk.chunkZ * 16;
        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    var block = section.getBlock(x, y, z);
                    if (block.hasBlockEntity()) {
                        var pos = new BlockPos(chunkBlockX + x, sectionBlockY + y, chunkBlockZ + z);
                        chunk.getBlockEntity(pos, WorldChunk.BlockEntityCreationType.IMMEDIATE);
                    }
                }
            }
        }
    }
    *///?}

    @Override
    protected @Nullable ChunkBuilderTask<ChunkBuildOutput> createRebuildTask(RenderSection render, int frame) {
        if (isSectionVisuallyEmpty(render.getChunkX(), render.getChunkY(), render.getChunkZ())) {
            return null;
        }

        ChunkRenderContext context = new ChunkRenderContext(new SectionPos(render.getChunkX(), render.getChunkY(), render.getChunkZ()));

        // TODO: This is a workaround until we properly snapshot chunk sections as is done in 1.12
        //? if >=1.8
        /*populateTileEntities(this.world.getChunkAt(render.getChunkX(), render.getChunkZ()), render.getChunkY());*/

        return new ChunkBuilderMeshingTask(render, context, frame, this.cameraPosition);
    }

    @Override
    protected boolean allowImportantRebuilds() {
        return false;
    }

    @Override
    protected boolean updateSectionInfo(RenderSection render, @Nullable BuiltRenderSectionData info) {
        boolean changed = super.updateSectionInfo(render, info);

        if (changed) {
            if (!(info instanceof PrimitiveBuiltRenderSectionData data)) {
                this.sectionsWithSkyLight.remove(render);
            } else if (data.hasSkyLight) {
                this.sectionsWithSkyLight.add(render);
            }
        }

        return changed;
    }

    public Collection<RenderSection> getSectionsWithSkyLight() {
        return this.sectionsWithSkyLight;
    }

    private static class ChunkRenderer extends DefaultChunkRenderer {

        public ChunkRenderer(RenderDevice device, RenderPassConfiguration<?> renderPassConfiguration) {
            super(device, renderPassConfiguration);
        }

        @Override
        protected void configureShaderInterface(ChunkShaderInterface shader) {
            shader.setTextureSlot(ChunkShaderTextureSlot.BLOCK, 0);
            shader.setTextureSlot(ChunkShaderTextureSlot.LIGHT, 1);
        }
    }
}
