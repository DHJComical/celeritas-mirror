package org.embeddedt.embeddium.impl.modern.render.chunk;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import it.unimi.dsi.fastutil.objects.ReferenceSets;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.embeddedt.embeddium.api.ChunkMeshEvent;
import org.embeddedt.embeddium.api.render.texture.SpriteUtil;
import org.embeddedt.embeddium.impl.Celeritas;
import org.embeddedt.embeddium.impl.gl.device.CommandList;
import org.embeddedt.embeddium.impl.modern.render.chunk.compile.ModernChunkBuildContext;
import org.embeddedt.embeddium.impl.modern.render.chunk.compile.tasks.ChunkBuilderMeshingTask;
import org.embeddedt.embeddium.impl.modern.render.chunk.config.ModernRenderPassConfigurationBuilder;
import org.embeddedt.embeddium.impl.render.chunk.RenderPassConfiguration;
import org.embeddedt.embeddium.impl.render.chunk.RenderSection;
import org.embeddedt.embeddium.impl.render.chunk.RenderSectionManager;
import org.embeddedt.embeddium.impl.render.chunk.data.BuiltRenderSectionData;
import org.embeddedt.embeddium.impl.render.chunk.data.MinecraftBuiltRenderSectionData;
import org.embeddedt.embeddium.impl.render.chunk.lists.ChunkRenderList;
import org.embeddedt.embeddium.impl.render.chunk.lists.SectionTicker;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.AsyncOcclusionMode;
import org.embeddedt.embeddium.impl.render.chunk.sprite.GenericSectionSpriteTicker;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexType;
import org.embeddedt.embeddium.impl.render.viewport.Viewport;
import org.embeddedt.embeddium.impl.util.WorldUtil;
import org.embeddedt.embeddium.impl.util.sodium.FlawlessFrames;
import org.embeddedt.embeddium.impl.world.WorldSlice;
import org.embeddedt.embeddium.impl.world.cloned.ChunkRenderContext;
import org.embeddedt.embeddium.impl.world.cloned.ClonedChunkSectionCache;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class ModernRenderSectionManager extends RenderSectionManager {
    private final ClientLevel world;
    @Getter
    private final ClonedChunkSectionCache sectionCache;

    private final ReferenceSet<RenderSection> sectionsWithGlobalEntities = new ReferenceOpenHashSet<>();

    protected ModernRenderSectionManager(RenderPassConfiguration<RenderType> configuration, ClientLevel world, int renderDistance, CommandList commandList) {
        super(configuration,
                () -> new ModernChunkBuildContext(world, configuration),
                ModernChunkRenderer::new,
                renderDistance,
                commandList,
                WorldUtil.getMinSection(world),
                WorldUtil.getMaxSection(world),
                Celeritas.options().performance.chunkBuilderThreads);
        this.world = world;
        this.sectionCache = new ClonedChunkSectionCache(this.world);
    }

    public static ModernRenderSectionManager create(ChunkVertexType vertexType, ClientLevel world, int renderDistance, CommandList commandList) {
        var renderPassConfiguration = ModernRenderPassConfigurationBuilder.build(vertexType);
        return new ModernRenderSectionManager(renderPassConfiguration, world, renderDistance, commandList);
    }

    protected boolean isSectionVisuallyEmpty(int x, int y, int z) {
        ChunkAccess chunk = this.world.getChunk(x, z);
        LevelChunkSection section = chunk.getSections()[WorldUtil.getSectionIndexFromSectionY(this.world, y)];

        return WorldUtil.isSectionEmpty(section) && ChunkMeshEvent.post(this.world, SectionPos.of(x, y, z)).isEmpty();
    }

    //? if shaders {
    @Override
    public boolean isInShadowPass() {
        return net.irisshaders.iris.shadows.ShadowRenderingState.areShadowsCurrentlyBeingRendered();
    }
    //?}

    @Override
    protected AsyncOcclusionMode getAsyncOcclusionMode() {
        return Celeritas.options().performance.asyncOcclusionMode;
    }

    @Override
    protected boolean shouldUseOcclusionCulling(Viewport viewport, boolean spectator) {
        final boolean useOcclusionCulling;
        var camBlockPos = viewport.getBlockCoord();
        BlockPos origin = new BlockPos(camBlockPos.x(), camBlockPos.y(), camBlockPos.z());

        if (spectator && this.world.getBlockState(origin)
                .isSolidRender(/*? if <1.21.2 {*/this.world, origin/*?}*/))
        {
            useOcclusionCulling = false;
        } else {
            useOcclusionCulling = Minecraft.getInstance().smartCull;
        }
        return useOcclusionCulling;
    }

    @Override
    public void updateChunks(boolean updateImmediately) {
        this.sectionCache.cleanup();
        super.updateChunks(updateImmediately);
    }

    @Override
    protected @Nullable ChunkBuilderMeshingTask createRebuildTask(RenderSection render, int frame) {
        ChunkRenderContext context = WorldSlice.prepare(this.world, SectionPos.of(render.getChunkX(), render.getChunkY(), render.getChunkZ()), this.sectionCache);

        if (context == null) {
            return null;
        }

        return new ChunkBuilderMeshingTask(render, context, frame, this.cameraPosition);
    }

    @Override
    protected void scheduleSectionForRebuild(int x, int y, int z, boolean important) {
        this.sectionCache.invalidate(x, y, z);
        super.scheduleSectionForRebuild(x, y, z, important);
    }

    @Override
    protected @Nullable SectionTicker createSectionTicker() {
        return new GenericSectionSpriteTicker<>(SpriteUtil::markSpriteActive);
    }

    @Override
    protected void updateSectionInfo(RenderSection render, BuiltRenderSectionData info) {
        super.updateSectionInfo(render, info);

        if (info == null || (info instanceof MinecraftBuiltRenderSectionData<?, ?> mcInfo) && mcInfo.globalBlockEntities.isEmpty()) {
            this.sectionsWithGlobalEntities.remove(render);
        } else {
            this.sectionsWithGlobalEntities.add(render);
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        this.sectionsWithGlobalEntities.clear();
    }

    public Collection<RenderSection> getSectionsWithGlobalEntities() {
        return ReferenceSets.unmodifiable(this.sectionsWithGlobalEntities);
    }

    @Override
    protected boolean useFogOcclusion() {
        return Celeritas.options().performance.useFogOcclusion;
    }

    @Override
    protected boolean allowImportantRebuilds() {
        return !Celeritas.options().performance.alwaysDeferChunkUpdates;
    }

    @Override
    protected boolean shouldRespectUpdateTaskQueueSizeLimit() {
        return !FlawlessFrames.isActive();
    }
}
