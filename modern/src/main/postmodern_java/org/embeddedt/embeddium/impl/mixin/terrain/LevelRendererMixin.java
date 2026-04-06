package org.embeddedt.embeddium.impl.mixin.terrain;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.data.AtlasIds;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.phys.Vec3;
import org.embeddedt.embeddium.impl.gl.device.RenderDevice;
import org.embeddedt.embeddium.impl.render.CeleritasWorldRenderer;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderFogComponent;
import org.embeddedt.embeddium.impl.render.terrain.SimpleWorldRenderer;
import org.embeddedt.embeddium.impl.render.viewport.ViewportProvider;
import org.embeddedt.embeddium.impl.world.WorldRendererExtended;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.EnumMap;
import java.util.SortedSet;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin implements WorldRendererExtended {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private SectionOcclusionGraph sectionOcclusionGraph;

    @Shadow
    @Final
    private Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress;
    @Shadow
    @Final
    private LevelRenderState levelRenderState;
    @Unique
    private CeleritasWorldRenderer celeritas$renderer;

    @Unique
    private int frame;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void initializeRenderer(CallbackInfo ci) {
        this.celeritas$renderer = new CeleritasWorldRenderer(this.minecraft);
    }

    @Redirect(method = "allChanged()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;getEffectiveRenderDistance()I", ordinal = 1))
    private int nullifyBuiltChunkStorage(Options options) {
        // Do not allow any resources to be allocated
        return 0;
    }

    @Inject(method = "allChanged()V", at = @At("RETURN"))
    private void onReload(CallbackInfo ci) {
        RenderDevice.enterManagedCode();

        try {
            this.celeritas$renderer.reload();
        } finally {
            RenderDevice.exitManagedCode();
        }
    }

    @Inject(method = "setLevel", at = @At("HEAD"))
    private void onWorldChanged(ClientLevel world, CallbackInfo ci) {
        RenderDevice.enterManagedCode();

        try {
            this.celeritas$renderer.setWorld(world);
        } finally {
            RenderDevice.exitManagedCode();
        }
    }

    @Overwrite
    private void cullTerrain(Camera camera, Frustum frustum, boolean spectator) {
        var viewport = ((ViewportProvider) frustum).sodium$createViewport();

        if (this.sectionOcclusionGraph.consumeFrustumUpdate()) {
            this.celeritas$renderer.scheduleTerrainUpdate();
        }

        Vec3 pos = camera.position();
        float pitch = camera.xRot();
        float yaw = camera.yRot();
        float fogDistance = ChunkShaderFogComponent.FOG_SERVICE.getFogCutoff();

        var cameraState = new SimpleWorldRenderer.CameraState(
                pos.x, pos.y, pos.z,
                pitch, yaw, fogDistance
        );

        RenderDevice.enterManagedCode();

        try {
            this.celeritas$renderer.setupTerrain(viewport, cameraState, this.frame++, spectator, false);
        } finally {
            RenderDevice.exitManagedCode();
        }
    }

    /**
     * @author embeddedt
     * @reason Celeritas renders the chunks itself, so we return a blank list here.
     */
    @Overwrite
    public ChunkSectionsToRender prepareChunkRenders(Matrix4fc matrix) {
        var blocksAtlas = this.minecraft.getTextureManager().getTexture(AtlasIds.BLOCKS).getTextureView();
        celeritas$renderer.setCurrentChunkRenderPose(new Matrix4f(matrix));
        return new ChunkSectionsToRender(blocksAtlas, new EnumMap<>(ChunkSectionLayer.class), 0, new GpuBufferSlice[0]);
    }

    @Redirect(method = "lambda$addMainPass$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;renderGroup(Lnet/minecraft/client/renderer/chunk/ChunkSectionLayerGroup;Lcom/mojang/blaze3d/textures/GpuSampler;)V"))
    private void renderChunksWithCeleritas(ChunkSectionsToRender sectionsToRender, ChunkSectionLayerGroup group, GpuSampler terrainSampler) {
        RenderDevice.enterManagedCode();
        var cameraPos = this.levelRenderState.cameraRenderState.pos;
        try {
            for (ChunkSectionLayer layer : group.layers()) {
                celeritas$renderer.drawChunkLayer(layer, cameraPos.x, cameraPos.y, cameraPos.z);
            }
        } finally {
            RenderDevice.exitManagedCode();
        }
    }

    @Inject(method = "extractVisibleBlockEntities(Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/state/level/LevelRenderState;Lnet/minecraft/client/renderer/culling/Frustum;)V", at = @At("RETURN"))
    private void extractCeleritasBlockEntities(Camera camera, float partialTick, LevelRenderState renderState, Frustum frustum, CallbackInfo ci, @Local(ordinal = 0) boolean showOutlines) {
        var cameraPos = camera.position();
        celeritas$renderer.renderBlockEntities(new CeleritasWorldRenderer.BlockEntityRenderContext(
                new PoseStack(),
                renderState,
                cameraPos.x,
                cameraPos.y,
                cameraPos.z,
                this.destructionProgress,
                partialTick,
                null
        ));
        if (showOutlines && celeritas$renderer.didBlockEntityRequestOutline()) {
            renderState.haveGlowingEntities = true;
        }
    }

    @Override
    public CeleritasWorldRenderer sodium$getWorldRenderer() {
        return celeritas$renderer;
    }

    @Overwrite
    public boolean isSectionCompiledAndVisible(BlockPos pos) {
        return this.celeritas$renderer.isSectionReady(
                SectionPos.blockToSectionCoord(pos.getX()),
                SectionPos.blockToSectionCoord(pos.getY()),
                SectionPos.blockToSectionCoord(pos.getZ())
        );
    }

    /**
     * @reason Redirect chunk updates to our renderer
     * @author JellySquid
     */
    @Overwrite
    public void setBlocksDirty(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.celeritas$renderer.scheduleRebuildForBlockArea(minX, minY, minZ, maxX, maxY, maxZ, false);
    }

    /**
     * @reason Redirect chunk updates to our renderer
     * @author JellySquid
     */
    @Overwrite
    public void setSectionDirtyWithNeighbors(int x, int y, int z) {
        this.celeritas$renderer.scheduleRebuildForChunks(x - 1, y - 1, z - 1, x + 1, y + 1, z + 1, false);
    }

    /**
     * @reason Redirect chunk updates to our renderer
     * @author JellySquid
     */
    @Overwrite
    private void setBlockDirty(BlockPos pos, boolean important) {
        this.celeritas$renderer.scheduleRebuildForBlockArea(pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1, pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1, important);
    }

    /**
     * @reason Redirect chunk updates to our renderer
     * @author JellySquid
     */
    @Overwrite
    private void setSectionDirty(int x, int y, int z, boolean important) {
        this.celeritas$renderer.scheduleRebuildForChunk(x, y, z, important);
    }
}
