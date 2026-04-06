package org.embeddedt.embeddium.impl.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import lombok.Setter;
//? if shaders
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
//? if >=1.21.11
/*import net.minecraft.client.renderer.feature.ModelFeatureRenderer;*/
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import org.embeddedt.embeddium.impl.Celeritas;
import org.embeddedt.embeddium.impl.gl.device.CommandList;
import org.embeddedt.embeddium.impl.model.color.BlendedColorProvider;
//? if <1.21.11
import org.embeddedt.embeddium.impl.modern.render.chunk.ChunkRenderMatricesBuilder;
import org.embeddedt.embeddium.impl.modern.render.chunk.ModernRenderSectionManager;
import org.embeddedt.embeddium.impl.render.chunk.ChunkRenderMatrices;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkMeshFormats;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexType;
import org.embeddedt.embeddium.impl.render.terrain.SimpleWorldRenderer;
import org.embeddedt.embeddium.impl.render.viewport.Viewport;
import org.embeddedt.embeddium.impl.util.ClientUtil;
import org.embeddedt.embeddium.impl.util.WorldUtil;
import org.embeddedt.embeddium.impl.world.WorldRendererExtended;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.List;
import java.util.Objects;
import java.util.SortedSet;
import java.util.function.Predicate;

public class CeleritasWorldRenderer extends SimpleWorldRenderer<ClientLevel,
        ModernRenderSectionManager,
        //? if <1.21.11 {
        net.minecraft.client.renderer.RenderType,
        //?} else {
        /*net.minecraft.client.renderer.chunk.ChunkSectionLayer,
        *///?}
        BlockEntity, CeleritasWorldRenderer.BlockEntityRenderContext> {
    private final Minecraft client;

    // We track whether a block entity uses custom block outline rendering, so that the outline postprocessing
    // shader will be enabled appropriately
    private boolean blockEntityRequestedOutline;

    @Setter
    private Matrix4f currentChunkRenderPose;

    //? if >=26.1 {
    /*@Setter
    private Matrix4f currentChunkRenderProjection;
    *///?}

    private boolean useEntityCulling;


    public CeleritasWorldRenderer(Minecraft client) {
        this.client = client;
    }

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

    @Override
    public int getMinimumBuildHeight() {
        return WorldUtil.getMinBuildHeight(this.world);
    }

    @Override
    public int getMaximumBuildHeight() {
        return WorldUtil.getMaxBuildHeight(this.world);
    }

    @Override
    protected void initRenderer(CommandList commandList) {
        super.initRenderer(commandList);

        // Forge workaround - reset VSync flag
        var window = Minecraft.getInstance().getWindow();
        if (window != null) {
            window.updateVsync(Minecraft.getInstance().options.enableVsync/*? if >=1.19 {*/().get()/*?}*/);
        }

        BlendedColorProvider.checkBlendingEnabled();
    }

    @Override
    public void setupTerrain(Viewport viewport, CameraState cameraState, int frame, boolean spectator, boolean updateChunksImmediately) {
        super.setupTerrain(viewport, cameraState, frame, spectator, updateChunksImmediately);

        double entityDistanceScale;

        //? if >=1.19 {
        entityDistanceScale = this.client.options.entityDistanceScaling().get();
        //?} else if >=1.16 {
        /*entityDistanceScale = this.client.options.entityDistanceScaling;
         *///?} else {
        /*entityDistanceScale = 1.0;
         *///?}

        Entity.setViewScale(Mth.clamp((double) getEffectiveRenderDistance() / 8.0D, 1.0D, 2.5D) * entityDistanceScale);

        this.useEntityCulling = Celeritas.options().performance.useEntityCulling;
    }

    @Override
    public int getEffectiveRenderDistance() {
        //? if >=1.18 {
        return Minecraft.getInstance().options.getEffectiveRenderDistance();
        //?} else
        /*return Minecraft.getInstance().options.renderDistance;*/
    }

    @Override
    protected ChunkRenderMatrices createChunkRenderMatrices() {
        //? if <1.21.11 {
        return ChunkRenderMatricesBuilder.from(Objects.requireNonNull(currentChunkRenderPose, "chunk render pose not set"));
        //?} else {
        /*return new ChunkRenderMatrices(new Matrix4f(currentChunkRenderProjection), new Matrix4f(currentChunkRenderPose));
        *///?}
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

    @Override
    protected ModernRenderSectionManager createRenderSectionManager(CommandList commandList) {
        return ModernRenderSectionManager.create(chooseVertexType(), this.world, this.renderDistance, commandList);
    }

    @Override
    public int renderBlockEntities(BlockEntityRenderContext blockEntityRenderContext) {
        this.blockEntityRequestedOutline = false;
        return super.renderBlockEntities(blockEntityRenderContext);
    }

    @Override
    protected void renderBlockEntityList(List<BlockEntity> list, BlockEntityRenderContext blockEntityRenderContext) {
        var blockEntityFilter = blockEntityRenderContext.blockEntityFilter();
        var viewport = this.currentViewport;
        //? if >=1.18 {
        var dispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
        //?} else
        /*var dispatcher = BlockEntityRenderDispatcher.instance;*/

        for (var blockEntity : list) {
            if (blockEntityFilter != null && !blockEntityFilter.test(blockEntity)) {
                continue;
            }

            // Disabled for now as it is relatively expensive to do virtual dispatch on getRenderBoundingBox when most
            // TEs fit within the 1x1x1 area, and are thus likely to be visible anyway
            /*
            var aabb = blockEntity.getRenderBoundingBox();
            if (!viewport.isBoxVisible(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ)) {
                continue;
            }
             */

            //? if forgelike && >=1.19.2 {
            if (blockEntity.hasCustomOutlineRendering(this.client.player)) {
                this.blockEntityRequestedOutline = true;
            }
            //?}

            renderBlockEntity(blockEntityRenderContext, dispatcher, blockEntity);
        }

    }

    private static void renderBlockEntity(BlockEntityRenderContext context,
                                          BlockEntityRenderDispatcher dispatcher,
                                          BlockEntity entity) {
        BlockPos pos = entity.getBlockPos();

        var matrices = context.pose();

        matrices.pushPose();
        matrices.translate((double) pos.getX() - context.x(), (double) pos.getY() - context.y(), (double) pos.getZ() - context.z());

        SortedSet<BlockDestructionProgress> breakingInfo = context.blockBreakingProgressions().get(pos.asLong());

        //? if <1.21.11 {
        MultiBufferSource immediate = context.renderBuffers().bufferSource();
        MultiBufferSource consumer = immediate;
        if (breakingInfo != null && !breakingInfo.isEmpty()) {
            int stage = breakingInfo.last().getProgress();

            if (stage >= 0) {
                var bufferBuilder = context.renderBuffers().crumblingBufferSource()
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
            dispatcher.render(entity, context.tickDelta(), matrices, consumer);
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
        //?} else {
        /*ModelFeatureRenderer.CrumblingOverlay crumblingOverlay;
        if (breakingInfo != null && !breakingInfo.isEmpty()) {
            crumblingOverlay = new ModelFeatureRenderer.CrumblingOverlay(breakingInfo.last().getProgress(), matrices.last());
        } else {
            crumblingOverlay = null;
        }
        // intentionally block the frustum culling
        var state = dispatcher.tryExtractRenderState(entity, context.tickDelta(), crumblingOverlay, null);
        if (state != null) {
            context.renderState().blockEntityRenderStates.add(state);
        }
        *///?}

        matrices.popPose();
    }

    public boolean didBlockEntityRequestOutline() {
        return blockEntityRequestedOutline;
    }

    /**
     * Returns whether or not the entity intersects with any visible chunks in the graph.
     * @return True if the entity is visible, otherwise false
     */
    public boolean isEntityVisible(Entity entity, AABB boundingBox) {
        if (!this.useEntityCulling || this.renderSectionManager.isInShadowPass()) {
            return true;
        }

        // Ensure entities with outlines or nametags are always visible
        if (ClientUtil.shouldEntityAppearGlowing(entity) || entity.shouldShowName()) {
            return true;
        }

        return this.isBoxVisible(boundingBox);
    }

    public boolean isBoxVisible(AABB box) {
        return this.isBoxVisible(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }

    public void setCurrentViewport(Viewport viewport) {
        this.currentViewport = viewport;
    }

    public record BlockEntityRenderContext(
            PoseStack pose,
            //? if <1.21.11 {
            RenderBuffers renderBuffers,
            //?} else
            /*net.minecraft.client.renderer.state.level.LevelRenderState renderState,*/
            double x,
            double y,
            double z,
            Long2ObjectMap<SortedSet<BlockDestructionProgress>> blockBreakingProgressions,
            float tickDelta,
            @Nullable Predicate<BlockEntity> blockEntityFilter
    ) {

    }
}
