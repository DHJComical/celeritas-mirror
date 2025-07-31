package org.taumc.celeritas.mixin.core;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.Lighting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.Culler;
import net.minecraft.client.render.world.WorldRenderer;
import net.minecraft.entity.living.LivingEntity;
import net.minecraft.world.World;
import org.embeddedt.embeddium.impl.gl.device.RenderDevice;
import org.embeddedt.embeddium.impl.render.viewport.ViewportProvider;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.taumc.celeritas.impl.extensions.RenderGlobalExtension;
import org.taumc.celeritas.impl.render.terrain.CeleritasWorldRenderer;
//? if <1.0.0-beta.8
/*import org.taumc.celeritas.impl.render.terrain.compile.PrimitiveBuiltRenderSectionData;*/

@Mixin(value = WorldRenderer.class, priority = 900)
public abstract class WorldRendererMixin implements RenderGlobalExtension {

    @Shadow
    private Minecraft minecraft;

    @Shadow
    private int chunkGridSizeX, chunkGridSizeY, chunkGridSizeZ;

    private CeleritasWorldRenderer renderer;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(CallbackInfo ci) {
        this.renderer = new CeleritasWorldRenderer();
    }

    @Override
    public CeleritasWorldRenderer sodium$getWorldRenderer() {
        return this.renderer;
    }

    @Inject(method = "setWorld", at = @At("RETURN"))
    private void onWorldChanged(@Coerce World world, CallbackInfo ci) {
        RenderDevice.enterManagedCode();

        try {
            this.renderer.setWorld(world);
        } finally {
            RenderDevice.exitManagedCode();
        }
    }

    @Inject(method = { "reload", "m_6748042" }, at = @At(opcode = Opcodes.PUTFIELD, value = "FIELD", target = "Lnet/minecraft/client/render/world/WorldRenderer;chunkGridSizeZ:I", shift = At.Shift.AFTER))
    private void nullifyBuiltChunkStorage(CallbackInfo ci) {
        this.chunkGridSizeX = 0;
        this.chunkGridSizeY = 0;
        this.chunkGridSizeZ = 0;
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

        //? if >=1.0.0-beta.8
        this.minecraft.gameRenderer.enableLightMap(ticks);

        try {
            this.renderer.drawChunkLayer(pass, d3, d4, d5);
        } finally {
            RenderDevice.exitManagedCode();
        }

        //? if >=1.0.0-beta.8
        this.minecraft.gameRenderer.disableLightMap(ticks);

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

    //? if <1.3 {
    private static final String ON_RELOAD = "m_6748042";
    //?} else
    /*private static final String ON_RELOAD = "reload";*/

    @Inject(method = ON_RELOAD, at = @At("RETURN"))
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
    public void sodium$renderTileEntities(CallbackInfo ci, @Local(ordinal = 0, argsOnly = true) float partialTicks) {
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
    /*/^*
     * @author embeddedt
     * @reason trigger chunk updates when sky light level changes
     ^/
    @Overwrite
    public void onAmbientDarknessChanged() {
        for (var section : this.renderer.getRenderSectionManager().getAllRenderSections()) {
            if (section.getBuiltContext() instanceof PrimitiveBuiltRenderSectionData data && data.hasSkyLight) {
                this.renderer.scheduleRebuildForChunk(section.getChunkX(), section.getChunkY(), section.getChunkZ(), false);
            }
        }
    }
    *///?}
}

