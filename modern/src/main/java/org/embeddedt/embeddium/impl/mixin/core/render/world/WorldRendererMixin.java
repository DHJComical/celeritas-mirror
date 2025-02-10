package org.embeddedt.embeddium.impl.mixin.core.render.world;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
//? if >=1.21
/*import net.minecraft.client.DeltaTracker;*/
import net.minecraft.client.renderer.*;
//? if neoforge
/*import net.neoforged.neoforge.client.ClientHooks;*/
import org.embeddedt.embeddium.api.math.JomlHelper;
import org.embeddedt.embeddium.impl.gl.device.RenderDevice;
import org.embeddedt.embeddium.impl.render.CeleritasWorldRenderer;
import org.embeddedt.embeddium.impl.render.viewport.ViewportProvider;
import org.embeddedt.embeddium.impl.world.WorldRendererExtended;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.BlockDestructionProgress;
//? if forge
import net.minecraftforge.client.ForgeHooksClient;
import org.embeddedt.embeddium.impl.util.sodium.FlawlessFrames;
//? if >=1.20 {
import org.joml.Matrix4f;
//?} else
/*import com.mojang.math.Matrix4f;*/
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(LevelRenderer.class)
public abstract class WorldRendererMixin implements WorldRendererExtended {
    @Shadow
    @Final
    private RenderBuffers renderBuffers;

    @Shadow
    @Final
    private Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress;

    //? if <1.18 {
    /*@Shadow
    private boolean needsUpdate;
    *///?} else if >=1.18 <1.20.2 {
    @Shadow
    private boolean needsFullRenderChunkUpdate;

    @Shadow
    @Final
    private AtomicBoolean needsFrustumUpdate;
    //?} else {
    /*@Shadow
    @Final
    private SectionOcclusionGraph sectionOcclusionGraph;
    *///?}

    @Shadow
    private int ticks;

    @Shadow
    @Final
    private Minecraft minecraft;

    @Unique
    private CeleritasWorldRenderer renderer;

    @Unique
    private int frame;

    @Shadow public abstract boolean shouldShowEntityOutlines();

    //? if >=1.18 {
    @Shadow
    @Nullable
    private Frustum capturedFrustum;

    @Shadow
    private Frustum cullingFrustum;

    @Unique
    private Frustum embeddium$getCurrentFrustum() {
        return this.capturedFrustum != null ? this.capturedFrustum : this.cullingFrustum;
    }
    //?}

    @Override
    public CeleritasWorldRenderer sodium$getWorldRenderer() {
        return this.renderer;
    }

    @Redirect(method = "allChanged()V", at =
        //? if >=1.18 {
        @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;getEffectiveRenderDistance()I", ordinal = 1)
        //?} else
        /*@At(value = "FIELD", target = "Lnet/minecraft/client/Options;renderDistance:I")*/
    )
    private int nullifyBuiltChunkStorage(Options options) {
        // Do not allow any resources to be allocated
        return 0;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(CallbackInfo ci, @Local(ordinal = 0, argsOnly = true) Minecraft client) {
        this.renderer = new CeleritasWorldRenderer(client);
    }

    @Inject(method = "setLevel", at = @At("HEAD"))
    private void onWorldChanged(ClientLevel world, CallbackInfo ci) {
        RenderDevice.enterManagedCode();

        try {
            this.renderer.setWorldWithoutReload(world);
        } finally {
            RenderDevice.exitManagedCode();
        }
    }

    /**
     * @reason Redirect to our renderer
     * @author JellySquid
     */
    @Overwrite
    public int /*? if <1.20.2 {*/ countRenderedChunks /*?} else {*/ /*countRenderedSections *//*?}*/() {
        return this.renderer.getVisibleChunkCount();
    }

    /**
     * @reason Redirect the check to our renderer
     * @author JellySquid
     */
    @Overwrite
    public boolean /*? if <1.20.2 {*/ hasRenderedAllChunks /*?} else {*/ /*hasRenderedAllSections *//*?}*/() {
        return this.renderer.isTerrainRenderComplete();
    }

    @Inject(method = "needsUpdate", at = @At("RETURN"))
    private void onTerrainUpdateScheduled(CallbackInfo ci) {
        this.renderer.scheduleTerrainUpdate();
    }

    /**
     * @reason Redirect the chunk layer render passes to our renderer
     * @author JellySquid
     */
    @Overwrite
    private void /*? if <1.20.2 {*/ renderChunkLayer /*?} else {*/ /*renderSectionLayer *//*?}*/(RenderType renderLayer, /*? if <1.20.6 {*/ PoseStack matrices, /*?}*/ double x, double y, double z /*? if >=1.20.6 {*/ /*,Matrix4f pose *//*?}*/ /*? if >=1.17 {*/, Matrix4f matrix /*?}*/) {
        RenderDevice.enterManagedCode();

        //? if >=1.20 <1.20.6 {
        Matrix4f pose = matrices.last().pose();
        //?} else if <1.20 {
        /*org.joml.Matrix4f pose = JomlHelper.copy(matrices.last().pose());
        *///?}

        try {
            this.renderer.drawChunkLayer(renderLayer, pose, x, y, z);
        } finally {
            RenderDevice.exitManagedCode();
        }

        // TODO: Avoid setting up and clearing the state a second time
        //? if forge && >=1.18 {
        renderLayer.setupRenderState();
        ForgeHooksClient.dispatchRenderStage(renderLayer, ((LevelRenderer)(Object)this),
                /*? if <1.20.6 {*/ matrices /*?} else {*/ /*pose *//*?}*/,
                matrix, this.ticks, this.minecraft.gameRenderer.getMainCamera(), this.embeddium$getCurrentFrustum());
        renderLayer.clearRenderState();
        //?}
        //? if neoforge && <1.21 {
        /*renderLayer.setupRenderState();
        ClientHooks.dispatchRenderStage(renderLayer, (LevelRenderer)(Object)this, matrices, matrix, this.ticks, this.minecraft.gameRenderer.getMainCamera(), this.embeddium$getCurrentFrustum());
        renderLayer.clearRenderState();
        *///?}
        //? if neoforge && >=1.21 {
        /*renderLayer.setupRenderState();
        ClientHooks.dispatchRenderStage(renderLayer, (LevelRenderer)(Object)this, pose, matrix, this.ticks, this.minecraft.gameRenderer.getMainCamera(), this.embeddium$getCurrentFrustum());
        renderLayer.clearRenderState();
        *///?}
    }

    /**
     * @reason Redirect the terrain setup phase to our renderer
     * @author JellySquid
     */
    @Overwrite
    private void setupRender(Camera camera, Frustum frustum, boolean hasForcedFrustum, /*? if <1.18 {*/ /*int frame, *//*?}*/ boolean spectator) {
        var viewport = ((ViewportProvider) frustum).sodium$createViewport();

        // Detect mods setting the vanilla update flags themselves
        //? if <1.18 {
        /*if (this.needsUpdate) {
        *///?} else if >=1.18 <1.20.2 {
        if (this.needsFullRenderChunkUpdate || this.needsFrustumUpdate.compareAndSet(true, false)) {
        //?} else
        /*if (this.sectionOcclusionGraph.consumeFrustumUpdate()) {*/
            this.renderer.scheduleTerrainUpdate();
        }

        RenderDevice.enterManagedCode();

        try {
            this.renderer.setupTerrain(camera, viewport, this.frame++, spectator, FlawlessFrames.isActive());
        } finally {
            RenderDevice.exitManagedCode();
        }

        // We set this because third-party mods may use it (to loop themselves), even if Vanilla does not.
        //? if <1.18
        /*this.needsUpdate = false;*/
        //? if >=1.18 <1.20.2
        this.needsFullRenderChunkUpdate = false;
    }

    /**
     * @reason Redirect chunk updates to our renderer
     * @author JellySquid
     */
    @Overwrite
    public void setBlocksDirty(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.renderer.scheduleRebuildForBlockArea(minX, minY, minZ, maxX, maxY, maxZ, false);
    }

    /**
     * @reason Redirect chunk updates to our renderer
     * @author JellySquid
     */
    @Overwrite
    public void setSectionDirtyWithNeighbors(int x, int y, int z) {
        this.renderer.scheduleRebuildForChunks(x - 1, y - 1, z - 1, x + 1, y + 1, z + 1, false);
    }

    /**
     * @reason Redirect chunk updates to our renderer
     * @author JellySquid
     */
    @Overwrite
    private void setBlockDirty(BlockPos pos, boolean important) {
        this.renderer.scheduleRebuildForBlockArea(pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1, pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1, important);
    }

    /**
     * @reason Redirect chunk updates to our renderer
     * @author JellySquid
     */
    @Overwrite
    private void setSectionDirty(int x, int y, int z, boolean important) {
        this.renderer.scheduleRebuildForChunk(x, y, z, important);
    }

    //? if >=1.18 {
    @Overwrite
    public boolean /*? if <1.20.2 {*/ isChunkCompiled /*?} else {*/ /*isSectionCompiled *//*?}*/(BlockPos pos) {
        return this.renderer.isSectionReady(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
    }
    //?}

    @Inject(method = "allChanged()V", at = @At("RETURN"))
    private void onReload(CallbackInfo ci) {
        RenderDevice.enterManagedCode();

        try {
            this.renderer.reload();
        } finally {
            RenderDevice.exitManagedCode();
        }
    }

    /**
     * @author embeddedt
     * @reason take over block entity rendering
     */
    //? if <1.21.2 {
    @Inject(method = "renderLevel", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;globalBlockEntities:Ljava/util/Set;", shift = At.Shift.BEFORE, ordinal = 0))
    private void onRenderBlockEntities(
            //? if <1.20.6
            PoseStack matrices,
            /*? if <1.21 {*/ float tickDelta, long limitTime, /*?} else {*/ /*DeltaTracker tracker, *//*?}*/
            boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightmapTextureManager, /*? if >=1.20.6 {*/ /*Matrix4f pose, *//*?}*/ Matrix4f positionMatrix, CallbackInfo ci) {
        //? if >=1.21
        /*float tickDelta = tracker.getGameTimeDeltaPartialTick(false);*/
        //? if >=1.20.6
        /*PoseStack matrices = new PoseStack();*/

        this.renderer.renderBlockEntities(matrices, this.renderBuffers, this.destructionProgress, camera, tickDelta, null);
    }
    //?} else {
    /*@Overwrite
    private void renderBlockEntities(PoseStack stack, MultiBufferSource.BufferSource bufferSource, MultiBufferSource.BufferSource bufferSource2, Camera camera, float partialTick) {
        this.renderer.renderBlockEntities(new PoseStack(), this.renderBuffers, this.destructionProgress, camera, partialTick, null);
    }
    *///?}

    //? if <1.21.2 {
    /**
     * Target the flag that selects whether or not to enable the entity outline shader, and enable it if
     * we rendered a block entity that requested it.
     *
     * NOTE: When updating Embeddium to newer versions of the game, this injection point must be checked.
     */
    @ModifyVariable(method = "renderLevel", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;globalBlockEntities:Ljava/util/Set;", shift = At.Shift.BEFORE, ordinal = 0), ordinal = 3)
    private boolean changeEntityOutlineFlag(boolean bl) {
        return bl || (this.renderer.didBlockEntityRequestOutline() && this.shouldShowEntityOutlines());
    }
    //?}

    /**
     * @reason Replace the debug string
     * @author JellySquid
     */
    @Overwrite
    public String /*? if <1.20.2 {*/ getChunkStatistics /*?} else {*/ /*getSectionStatistics *//*?}*/() {
        return this.renderer.getChunksDebugString();
    }
}
