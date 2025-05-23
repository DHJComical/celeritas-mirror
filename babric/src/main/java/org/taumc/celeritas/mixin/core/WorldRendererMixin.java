package org.taumc.celeritas.mixin.core;

import com.mojang.blaze3d.platform.Lighting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.Culler;
import net.minecraft.client.render.texture.TextureManager;
import net.minecraft.client.render.world.WorldRenderer;
import net.minecraft.entity.living.LivingEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.embeddedt.embeddium.impl.gl.device.RenderDevice;
import org.embeddedt.embeddium.impl.render.chunk.RenderSection;
import org.embeddedt.embeddium.impl.render.viewport.ViewportProvider;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.taumc.celeritas.impl.extensions.RenderGlobalExtension;
import org.taumc.celeritas.impl.render.terrain.CeleritasWorldRenderer;
import org.taumc.celeritas.impl.render.terrain.compile.PrimitiveBuiltRenderSectionData;

@Mixin(value = WorldRenderer.class, priority = 900)
public abstract class WorldRendererMixin implements RenderGlobalExtension {

    @Shadow
    private Minecraft minecraft;
    private CeleritasWorldRenderer renderer;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(Minecraft arg, TextureManager par2, CallbackInfo ci) {
        this.renderer = new CeleritasWorldRenderer(arg);
    }

    @Override
    public CeleritasWorldRenderer sodium$getWorldRenderer() {
        return this.renderer;
    }

    @Inject(method = "setWorld", at = @At("RETURN"))
    private void onWorldChanged(World world, CallbackInfo ci) {
        RenderDevice.enterManagedCode();

        try {
            this.renderer.setWorld(world);
        } finally {
            RenderDevice.exitManagedCode();
        }
    }

    /**
     * @reason Redirect the chunk layer render passes to our renderer
     * @author JellySquid
     */
    @Overwrite
    public int render(LivingEntity viewEntity, int pass, double ticks) {
        // Allow FalseTweaks mixin to replace constant
        @SuppressWarnings("unused")
        double magicSortingConstantValue = 1.0D;
        RenderDevice.enterManagedCode();

        Lighting.turnOff();

        double d3 = viewEntity.prevTickX + (viewEntity.x - viewEntity.prevTickX) * ticks;
        // Do not apply eye height here or weird offsets will happen
        double d4 = viewEntity.prevTickY + (viewEntity.y - viewEntity.prevTickY) * ticks;
        double d5 = viewEntity.prevTickZ + (viewEntity.z - viewEntity.prevTickZ) * ticks;

        try {
            this.renderer.drawChunkLayer(pass, d3, d4, d5);
        } finally {
            RenderDevice.exitManagedCode();
        }

        //this.client.entityRenderer.disableLightmap(partialTicks);

        return 1;
    }

    @Unique
    private int frame = 0;

    /**
     * @reason Redirect the terrain setup phase to our renderer
     * @author JellySquid
     */
    @Overwrite
    public void updateFrustums(Culler camera, float tick) {
        RenderDevice.enterManagedCode();

        try {
            this.renderer.setupTerrain(((ViewportProvider)camera).sodium$createViewport(), tick, this.frame++, this.minecraft.player.noClip, false);
        } finally {
            RenderDevice.exitManagedCode();
        }
    }

    /**
     * @reason Redirect chunk updates to our renderer
     * @author JellySquid
     */
    @Overwrite
    public void markDirty(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.renderer.scheduleRebuildForBlockArea(minX, minY, minZ, maxX, maxY, maxZ, false);
    }

    @Inject(method = "m_6748042", at = @At("RETURN"))
    private void onReload(CallbackInfo ci) {
        RenderDevice.enterManagedCode();

        try {
            this.renderer.reload();
        } finally {
            RenderDevice.exitManagedCode();
        }
    }

    @Overwrite
    public boolean compileChunks(LivingEntity camera, boolean force) {
        return true;
    }

    @Inject(method = "renderEntities", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/world/WorldRenderer;globalBlockEntities:Ljava/util/List;", ordinal = 0))
    public void sodium$renderTileEntities(Vec3d cameraPos, Culler culler, float partialTicks, CallbackInfo ci) {
        this.renderer.renderBlockEntities(partialTicks);
    }

    /**
     * @reason Replace the debug string
     * @author JellySquid
     */
    @Overwrite
    public String getChunkDebugInfo() {
        return this.renderer.getChunksDebugString();
    }

    //? if <1.0.0-beta.8.1 {
    /**
     * @author embeddedt
     * @reason trigger chunk updates when sky light level changes
     */
    @Overwrite
    public void onAmbientDarknessChanged() {
        for (RenderSection section : this.renderer.getRenderSectionManager().getAllRenderSections()) {
            if (section.getBuiltContext() instanceof PrimitiveBuiltRenderSectionData data && data.hasSkyLight) {
                this.renderer.scheduleRebuildForChunk(section.getChunkX(), section.getChunkY(), section.getChunkZ(), false);
            }
        }
    }
    //?}
}

