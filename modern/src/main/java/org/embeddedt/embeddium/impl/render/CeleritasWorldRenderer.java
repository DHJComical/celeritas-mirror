package org.embeddedt.embeddium.impl.render;

import com.google.common.collect.Iterators;
import com.mojang.blaze3d.vertex.PoseStack;
//? if >=1.16 {
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
//?} else
/*import com.mojang.blaze3d.vertex.BreakingTextureGenerator;*/
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import lombok.Getter;
import lombok.Setter;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.embeddedt.embeddium.impl.Celeritas;
import org.embeddedt.embeddium.impl.common.util.NativeBuffer;
import org.embeddedt.embeddium.impl.gl.device.CommandList;
import org.embeddedt.embeddium.impl.gl.device.RenderDevice;
import org.embeddedt.embeddium.impl.loader.common.LoaderServices;
import org.embeddedt.embeddium.impl.model.quad.blender.BlendedColorProvider;
import org.embeddedt.embeddium.impl.modern.render.chunk.ChunkRenderMatricesBuilder;
import org.embeddedt.embeddium.impl.modern.render.chunk.ModernRenderSectionManager;
import org.embeddedt.embeddium.impl.render.chunk.ChunkRenderMatrices;
import org.embeddedt.embeddium.impl.render.chunk.RenderPassConfiguration;
import org.embeddedt.embeddium.impl.render.chunk.data.MinecraftBuiltRenderSectionData;
import org.embeddedt.embeddium.impl.render.chunk.lists.ChunkRenderList;
import org.embeddedt.embeddium.impl.render.chunk.lists.SortedRenderLists;
import org.embeddedt.embeddium.impl.render.chunk.map.ChunkStatus;
import org.embeddedt.embeddium.impl.render.chunk.map.ChunkTracker;
import org.embeddedt.embeddium.impl.render.chunk.map.ChunkTrackerHolder;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderFogComponent;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkMeshFormats;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexType;
import org.embeddedt.embeddium.impl.render.viewport.Viewport;
import org.embeddedt.embeddium.impl.util.*;
import org.embeddedt.embeddium.impl.world.WorldRendererExtended;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static org.embeddedt.embeddium.compat.mc.PlatformUtilService.PLATFORM_UTIL;

/**
 * Provides an extension to vanilla's {@link LevelRenderer}.
 */
public class CeleritasWorldRenderer {
    private static final boolean ENABLE_BLOCKENTITY_CULLING = PLATFORM_UTIL.modPresent("valkyrienskies");

    private final Minecraft client;

    private ClientLevel world;
    private int renderDistance;

    private double lastCameraX, lastCameraY, lastCameraZ;
    private double lastCameraPitch, lastCameraYaw;
    private float lastFogDistance;

    private boolean useEntityCulling;

    @Setter
    private Viewport currentViewport;

    @Getter
    private ModernRenderSectionManager renderSectionManager;

    /**
     * @return The CeleritasWorldRenderer based on the current dimension
     */
    public static CeleritasWorldRenderer instance() {
        var instance = instanceNullable();

        if (instance == null) {
            throw new IllegalStateException("No renderer attached to active world");
        }

        return instance;
    }

    /**
     * @return The CeleritasWorldRenderer based on the current dimension, or null if none is attached
     */
    public static CeleritasWorldRenderer instanceNullable() {
        var world = Minecraft.getInstance().levelRenderer;

        if (world instanceof WorldRendererExtended) {
            return ((WorldRendererExtended) world).sodium$getWorldRenderer();
        }

        return null;
    }

    public CeleritasWorldRenderer(Minecraft client) {
        this.client = client;
    }

    public void setWorldWithoutReload(ClientLevel world) {
        // Check that the world is actually changing
        if (this.world == world) {
            return;
        }

        // If we have a world is already loaded, unload the renderer
        if (this.world != null) {
            this.unloadWorld();
        }

        this.world = world;
    }

    private void unloadWorld() {
        if (this.renderSectionManager != null) {
            this.renderSectionManager.destroy();
            this.renderSectionManager = null;
        }

        this.world = null;
    }

    /**
     * @return The number of chunk renders which are visible in the current camera's frustum
     */
    public int getVisibleChunkCount() {
        return this.renderSectionManager.getVisibleChunkCount();
    }

    /**
     * Notifies the chunk renderer that the graph scene has changed and should be re-computed.
     */
    public void scheduleTerrainUpdate() {
        // BUG: seems to be called before init
        if (this.renderSectionManager != null) {
            this.renderSectionManager.markGraphDirty();
        }
    }

    /**
     * @return True if no chunks are pending rebuilds
     */
    public boolean isTerrainRenderComplete() {
        return this.renderSectionManager.getBuilder().isBuildQueueEmpty();
    }

    public static int getEffectiveRenderDistance() {
        //? if >=1.18 {
        return Minecraft.getInstance().options.getEffectiveRenderDistance();
        //?} else
        /*return Minecraft.getInstance().options.renderDistance;*/
    }

    /**
     * Called prior to any chunk rendering in order to update necessary state.
     */
    public void setupTerrain(Camera camera,
                             Viewport viewport,
                             @Deprecated(forRemoval = true) int frame,
                             boolean spectator,
                             boolean updateChunksImmediately) {
        NativeBuffer.reclaim(false);

        this.renderSectionManager.finishAllGraphUpdates();

        boolean isShadowPass = this.renderSectionManager.isInShadowPass();

        // Skip some unnecessary work in the shadow pass
        if (!isShadowPass) {
            this.processChunkEvents();

            this.renderSectionManager.runAsyncTasks();

            this.useEntityCulling = Celeritas.options().performance.useEntityCulling;

            if (getEffectiveRenderDistance() != this.renderDistance) {
                this.reload();
            }
        }

        ProfilerFiller profiler = ProfilerUtil.get();
        profiler.push("camera_setup");

        LocalPlayer player = this.client.player;

        if (player == null) {
            throw new IllegalStateException("Client instance has no active player entity");
        }

        Vec3 pos = camera.getPosition();
        float pitch = camera.getXRot();
        float yaw = camera.getYRot();
        float fogDistance = ChunkShaderFogComponent.FOG_SERVICE.getFogCutoff();

        boolean dirty = pos.x != this.lastCameraX || pos.y != this.lastCameraY || pos.z != this.lastCameraZ ||
                pitch != this.lastCameraPitch || yaw != this.lastCameraYaw || fogDistance != this.lastFogDistance;

        if (dirty) {
            this.renderSectionManager.markGraphDirty();
        }

        this.currentViewport = viewport;

        this.lastCameraX = pos.x;
        this.lastCameraY = pos.y;
        this.lastCameraZ = pos.z;
        this.lastCameraPitch = pitch;
        this.lastCameraYaw = yaw;
        this.lastFogDistance = fogDistance;

        profiler.popPush("chunk_update");

        this.renderSectionManager.updateChunks(updateChunksImmediately);

        // We don't need to upload chunks during shadow, they will be uploaded on the next real frame.
        if (!isShadowPass) {
            profiler.popPush("chunk_upload");

            this.renderSectionManager.uploadChunks();
        }

        // TODO: detect sun not moving and skip update during shadow pass
        if (this.renderSectionManager.needsUpdate() || isShadowPass) {
            profiler.popPush("chunk_render_lists");

            this.renderSectionManager.update(viewport, frame, spectator);
        }

        if (updateChunksImmediately) {
            profiler.popPush("chunk_upload_immediately");

            this.renderSectionManager.uploadChunks();
        }

        profiler.popPush("chunk_render_tick");

        this.renderSectionManager.tickVisibleRenders();

        profiler.pop();

        double entityDistanceScale;

        //? if >=1.19 {
        entityDistanceScale = this.client.options.entityDistanceScaling().get();
        //?} else if >=1.16 {
        /*entityDistanceScale = this.client.options.entityDistanceScaling;
        *///?} else {
        /*entityDistanceScale = 1.0;
        *///?}

        Entity.setViewScale(Mth.clamp((double) getEffectiveRenderDistance() / 8.0D, 1.0D, 2.5D) * entityDistanceScale);
    }

    private void processChunkEvents() {
        var tracker = ChunkTrackerHolder.get(this.world);
        tracker.forEachEvent(this.renderSectionManager::onChunkAdded, this.renderSectionManager::onChunkRemoved);
    }

    /**
     * Performs a render pass for the given {@link RenderType} and draws all visible chunks for it.
     */
    public void drawChunkLayer(RenderType renderLayer, Matrix4f pose, double x, double y, double z) {
        ChunkRenderMatrices matrices = ChunkRenderMatricesBuilder.from(pose);

        Collection<TerrainRenderPass> passes = this.renderSectionManager.getRenderPassConfiguration().vanillaRenderStages().get(renderLayer);

        if (passes != null && !passes.isEmpty()) {
            for (var pass : passes) {
                this.renderSectionManager.renderLayer(matrices, pass, x, y, z);
            }
        }
    }

    public void reload() {
        if (this.world == null) {
            return;
        }

        try (CommandList commandList = RenderDevice.INSTANCE.createCommandList()) {
            this.initRenderer(commandList);
        }
    }

    private ChunkVertexType chooseVertexType() {
        //? if shaders {
        if (WorldRenderingSettings.INSTANCE.shouldUseExtendedVertexFormat()) {
            return net.irisshaders.iris.compat.sodium.impl.vertex_format.IrisModelVertexFormats.MODEL_VERTEX_XHFP;
        }
        //?}

        if (Celeritas.canUseVanillaVertices()) {
            return ChunkMeshFormats.VANILLA_LIKE;
        }

        return ChunkMeshFormats.COMPACT;
    }

    private void initRenderer(CommandList commandList) {
        if (this.renderSectionManager != null) {
            this.renderSectionManager.destroy();
            this.renderSectionManager = null;
        }

        this.renderDistance = getEffectiveRenderDistance();

        this.renderSectionManager = ModernRenderSectionManager.create(chooseVertexType(), this.world, this.renderDistance, commandList);

        var tracker = ChunkTrackerHolder.get(this.world);
        ChunkTracker.forEachChunk(tracker.getReadyChunks(), this.renderSectionManager::onChunkAdded);

        // Forge workaround - reset VSync flag
        var window = Minecraft.getInstance().getWindow();
        if(window != null)
            window.updateVsync(Minecraft.getInstance().options.enableVsync/*? if >=1.19 {*/().get()/*?}*/);

        BlendedColorProvider.checkBlendingEnabled();
    }

    // We track whether a block entity uses custom block outline rendering, so that the outline postprocessing
    // shader will be enabled appropriately
    private boolean blockEntityRequestedOutline;

    public boolean didBlockEntityRequestOutline() {
        return blockEntityRequestedOutline;
    }

    /**
     * {@return an iterator over all visible block entities}
     * <p>
     * Note that this method performs significantly more allocations and will generally be less efficient than
     * {@link CeleritasWorldRenderer#forEachVisibleBlockEntity(Consumer)}. It is intended only for situations where using
     * that method is not feasible.
     */
    public Iterator<BlockEntity> blockEntityIterator() {
        return MinecraftBuiltRenderSectionData.generateBlockEntityIterator(this.renderSectionManager.getRenderLists(), this.renderSectionManager.getSectionsWithGlobalEntities());
    }

    public void forEachVisibleBlockEntity(Consumer<BlockEntity> consumer) {
        MinecraftBuiltRenderSectionData.forEachBlockEntity(consumer, this.renderSectionManager.getRenderLists(), this.renderSectionManager.getSectionsWithGlobalEntities());
    }

    public int renderBlockEntities(PoseStack poseStack,
                                    RenderBuffers bufferBuilders,
                                    Long2ObjectMap<SortedSet<BlockDestructionProgress>> blockBreakingProgressions,
                                    Camera camera,
                                    float tickDelta,
                                    @Nullable Predicate<BlockEntity> blockEntityFilter) {
        MultiBufferSource.BufferSource immediate = bufferBuilders.bufferSource();

        Vec3 cameraPos = camera.getPosition();
        double x = cameraPos.x();
        double y = cameraPos.y();
        double z = cameraPos.z();

        //? if >=1.17 {
        BlockEntityRenderDispatcher blockEntityRenderer = Minecraft.getInstance().getBlockEntityRenderDispatcher();
        //?} else
        /*BlockEntityRenderDispatcher blockEntityRenderer = BlockEntityRenderDispatcher.instance;*/

        this.blockEntityRequestedOutline = false;

        int numRendered = 0;

        numRendered += this.renderBlockEntities(poseStack, bufferBuilders, blockBreakingProgressions, tickDelta, immediate, x, y, z, blockEntityRenderer, blockEntityFilter);
        numRendered += this.renderGlobalBlockEntities(poseStack, bufferBuilders, blockBreakingProgressions, tickDelta, immediate, x, y, z, blockEntityRenderer, blockEntityFilter);

        return numRendered;
    }

    private static boolean isBoxVisible(Viewport viewport, AABB box) {
        if (!LoaderServices.INSTANCE.isCullableAABB(box)) {
            return true;
        }

        return viewport.isBoxVisible(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }

    private int renderBlockEntities(PoseStack matrices,
                                     RenderBuffers bufferBuilders,
                                     Long2ObjectMap<SortedSet<BlockDestructionProgress>> blockBreakingProgressions,
                                     float tickDelta,
                                     MultiBufferSource.BufferSource immediate,
                                     double x,
                                     double y,
                                     double z,
                                     BlockEntityRenderDispatcher blockEntityRenderer,
                                     @Nullable Predicate<BlockEntity> blockEntityFilter) {
        SortedRenderLists renderLists = this.renderSectionManager.getRenderLists();
        Iterator<ChunkRenderList> renderListIterator = renderLists.iterator();
        int numRendered = 0;

        while (renderListIterator.hasNext()) {
            var renderList = renderListIterator.next();

            var renderRegion = renderList.getRegion();
            var renderSectionIterator = renderList.sectionsWithEntitiesIterator();

            if (renderSectionIterator == null) {
                continue;
            }

            while (renderSectionIterator.hasNext()) {
                var renderSectionId = renderSectionIterator.nextByteAsInt();
                var renderSection = renderRegion.getSection(renderSectionId);

                if (renderSection == null) {
                    continue;
                }

                var context = renderSection.getBuiltContext();

                if (!(context instanceof MinecraftBuiltRenderSectionData mcData)) {
                    continue;
                }

                List<BlockEntity> blockEntities = mcData.culledBlockEntities;

                if (blockEntities.isEmpty()) {
                    continue;
                }

                for (BlockEntity blockEntity : blockEntities) {
                    if (blockEntityFilter != null && !blockEntityFilter.test(blockEntity)) {
                        continue;
                    }

                    //? if forge {
                    if(ENABLE_BLOCKENTITY_CULLING && !isBoxVisible(currentViewport, blockEntity.getRenderBoundingBox()))
                        continue;
                    //?}

                    //? if forgelike && >=1.19.2 && <1.21.2 {
                    if (blockEntity.hasCustomOutlineRendering(this.client.player)) {
                        this.blockEntityRequestedOutline = true;
                    }
                    //?}

                    renderBlockEntity(matrices, bufferBuilders, blockBreakingProgressions, tickDelta, immediate, x, y, z, blockEntityRenderer, blockEntity);
                    numRendered++;
                }
            }
        }

        return numRendered;
    }

    private int renderGlobalBlockEntities(PoseStack matrices,
                                           RenderBuffers bufferBuilders,
                                           Long2ObjectMap<SortedSet<BlockDestructionProgress>> blockBreakingProgressions,
                                           float tickDelta,
                                           MultiBufferSource.BufferSource immediate,
                                           double x,
                                           double y,
                                           double z,
                                           BlockEntityRenderDispatcher blockEntityRenderer,
                                           @Nullable Predicate<BlockEntity> blockEntityFilter) {
        int numRendered = 0;

        for (var renderSection : this.renderSectionManager.getSectionsWithGlobalEntities()) {
            var context = renderSection.getBuiltContext();

            if (!(context instanceof MinecraftBuiltRenderSectionData mcData)) {
                continue;
            }

            List<BlockEntity> blockEntities = mcData.globalBlockEntities;

            if (blockEntities.isEmpty()) {
                continue;
            }

            for (var blockEntity : blockEntities) {
                if (blockEntityFilter != null && !blockEntityFilter.test(blockEntity)) {
                    continue;
                }

                //? if forge {
                if(ENABLE_BLOCKENTITY_CULLING && !isBoxVisible(currentViewport, blockEntity.getRenderBoundingBox()))
                    continue;
                //?}

                //? if forgelike && >=1.19.2 && <1.21.2 {
                if (blockEntity.hasCustomOutlineRendering(this.client.player)) {
                    this.blockEntityRequestedOutline = true;
                }
                //?}

                renderBlockEntity(matrices, bufferBuilders, blockBreakingProgressions, tickDelta, immediate, x, y, z, blockEntityRenderer, blockEntity);
                numRendered++;
            }
        }

        return numRendered;
    }

    private static void renderBlockEntity(PoseStack matrices,
                                          RenderBuffers bufferBuilders,
                                          Long2ObjectMap<SortedSet<BlockDestructionProgress>> blockBreakingProgressions,
                                          float tickDelta,
                                          MultiBufferSource.BufferSource immediate,
                                          double x,
                                          double y,
                                          double z,
                                          BlockEntityRenderDispatcher dispatcher,
                                          BlockEntity entity) {
        BlockPos pos = entity.getBlockPos();

        matrices.pushPose();
        matrices.translate((double) pos.getX() - x, (double) pos.getY() - y, (double) pos.getZ() - z);

        MultiBufferSource consumer = immediate;
        SortedSet<BlockDestructionProgress> breakingInfo = blockBreakingProgressions.get(pos.asLong());

        if (breakingInfo != null && !breakingInfo.isEmpty()) {
            int stage = breakingInfo.last().getProgress();

            if (stage >= 0) {
                var bufferBuilder = bufferBuilders.crumblingBufferSource()
                        .getBuffer(ModelBakery.DESTROY_TYPES.get(stage));

                PoseStack.Pose entry = matrices.last();
                //? if <1.16 {
                /*VertexConsumer transformer = new BreakingTextureGenerator(bufferBuilder, entry);
                *///?} else if >=1.16 <1.20 {
                /*VertexConsumer transformer = new SheetedDecalTextureGenerator(bufferBuilder, entry.pose(), entry.normal());
                *///?} else if >=1.20 <1.20.6 {
                VertexConsumer transformer = new SheetedDecalTextureGenerator(bufferBuilder, entry.pose(), entry.normal(), 1.0f);
                //?} else if >=1.20.6 {
                /*VertexConsumer transformer = new SheetedDecalTextureGenerator(bufferBuilder, entry, 1.0f);
                *///?}

                consumer = (layer) -> layer.affectsCrumbling() ? VertexMultiConsumer.create(transformer, immediate.getBuffer(layer)) : immediate.getBuffer(layer);
            }
        }

        try {
            dispatcher.render(entity, tickDelta, matrices, consumer);
        } catch(RuntimeException e) {
            // We catch errors from removed block entities here, because we often end up being faster
            // than vanilla, and rendering them when they wouldn't be rendered by vanilla, which can
            // cause crashes. However, we do not apply this suppression to regular rendering.
            if (!entity.isRemoved()) {
                throw e;
            } else {
                Celeritas.logger().error("Suppressing crash from removed block entity", e);
            }
        }

        matrices.popPose();
    }



    // the volume of a section multiplied by the number of sections to be checked at most
    private static final double MAX_ENTITY_CHECK_VOLUME = 16 * 16 * 16 * 15;

    /**
     * Returns whether or not the entity intersects with any visible chunks in the graph.
     * @return True if the entity is visible, otherwise false
     */
    public boolean isEntityVisible(Entity entity, EntityRenderer renderer) {
        if (!this.useEntityCulling || this.renderSectionManager.isInShadowPass()) {
            return true;
        }

        // Ensure entities with outlines or nametags are always visible
        if (ClientUtil.shouldEntityAppearGlowing(entity) || entity.shouldShowName()) {
            return true;
        }

        //? if <1.21.2
        AABB box = entity.getBoundingBoxForCulling();
        //? if >=1.21.2
        /*AABB box = renderer.getBoundingBoxForCulling(entity);*/

        return this.isBoxVisible(box);
    }

    public boolean isBoxVisible(AABB box) {
        // bail on very large entities to avoid checking many sections
        // this also implicitly checks for the box being infinitely large
        double entityVolume = (box.maxX - box.minX) * (box.maxY - box.minY) * (box.maxZ - box.minZ);
        if (entityVolume > MAX_ENTITY_CHECK_VOLUME) {
            // TODO: do a frustum check instead, even large entities aren't visible if they're outside the frustum
            return true;
        }

        return this.isBoxVisible(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }

    public boolean isBoxVisible(double x1, double y1, double z1, double x2, double y2, double z2) {
        // Boxes outside the valid world height will never map to a rendered chunk
        // Always render these boxes or they'll be culled incorrectly!
        if (y2 < WorldUtil.getMinBuildHeight(this.world) + 0.5D || y1 > WorldUtil.getMaxBuildHeight(this.world) - 0.5D) {
            return true;
        }

        int minX = PositionUtil.posToSectionCoord(x1 - 0.5D);
        int minY = PositionUtil.posToSectionCoord(y1 - 0.5D);
        int minZ = PositionUtil.posToSectionCoord(z1 - 0.5D);

        int maxX = PositionUtil.posToSectionCoord(x2 + 0.5D);
        int maxY = PositionUtil.posToSectionCoord(y2 + 0.5D);
        int maxZ = PositionUtil.posToSectionCoord(z2 + 0.5D);

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    if (this.renderSectionManager.isSectionVisible(x, y, z)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public String getChunksDebugString() {
        // C: visible/total D: distance
        // TODO: add dirty and queued counts
        return String.format("C: %d/%d D: %d", this.renderSectionManager.getVisibleChunkCount(), this.renderSectionManager.getTotalSections(), this.renderDistance);
    }

    public RenderPassConfiguration<?> getRenderPassConfiguration() {
        return this.renderSectionManager.getRenderPassConfiguration();
    }

    /**
     * Schedules chunk rebuilds for all chunks in the specified block region.
     */
    public void scheduleRebuildForBlockArea(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, boolean important) {
        this.scheduleRebuildForChunks(minX >> 4, minY >> 4, minZ >> 4, maxX >> 4, maxY >> 4, maxZ >> 4, important);
    }

    /**
     * Schedules chunk rebuilds for all chunks in the specified chunk region.
     */
    public void scheduleRebuildForChunks(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, boolean important) {
        for (int chunkX = minX; chunkX <= maxX; chunkX++) {
            for (int chunkY = minY; chunkY <= maxY; chunkY++) {
                for (int chunkZ = minZ; chunkZ <= maxZ; chunkZ++) {
                    this.scheduleRebuildForChunk(chunkX, chunkY, chunkZ, important);
                }
            }
        }
    }

    /**
     * Schedules a chunk rebuild for the render belonging to the given chunk section position.
     */
    public void scheduleRebuildForChunk(int x, int y, int z, boolean important) {
        this.renderSectionManager.scheduleRebuild(x, y, z, important);
    }

    public Collection<String> getDebugStrings() {
        return this.renderSectionManager.getDebugStrings();
    }

    public boolean isSectionReady(int x, int y, int z) {
        return this.renderSectionManager.isSectionBuilt(x, y, z);
    }

    // Legacy compatibility
    @Deprecated
    public void onChunkAdded(int x, int z) {
        var tracker = ChunkTrackerHolder.get(this.world);
        tracker.onChunkStatusAdded(x, z, ChunkStatus.FLAG_HAS_BLOCK_DATA);
    }

    @Deprecated
    public void onChunkLightAdded(int x, int z) {
        var tracker = ChunkTrackerHolder.get(this.world);
        tracker.onChunkStatusAdded(x, z, ChunkStatus.FLAG_HAS_LIGHT_DATA);
    }

    @Deprecated
    public void onChunkRemoved(int x, int z) {
        var tracker = ChunkTrackerHolder.get(this.world);
        tracker.onChunkStatusRemoved(x, z, ChunkStatus.FLAG_ALL);
    }
}
