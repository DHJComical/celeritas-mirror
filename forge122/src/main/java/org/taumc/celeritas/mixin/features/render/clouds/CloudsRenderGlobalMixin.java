package org.taumc.celeritas.mixin.features.render.clouds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.resources.IResourceManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.taumc.celeritas.impl.render.clouds.VintageCloudRenderer;

@Mixin(RenderGlobal.class)
public class CloudsRenderGlobalMixin {
    @Shadow
    @Final
    private Minecraft mc;

    @Shadow
    private WorldClient world;

    @Shadow
    private int cloudTickCounter;

    @Unique
    private VintageCloudRenderer celeritas$cloudRenderer;

    @Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
    private void celeritas$renderCloudsFast(float partialTicks, int pass, double x, double y, double z, CallbackInfo ci) {
        if (!VintageCloudRenderer.isEnabled()) {
            return;
        }

        // Dimensions with their own cloud renderer keep it
        if (this.world.provider.getCloudRenderer() != null) {
            return;
        }

        ci.cancel();

        // Vanilla only draws clouds in surface worlds
        if (!this.world.provider.isSurfaceWorld()) {
            return;
        }

        if (this.celeritas$cloudRenderer == null) {
            this.celeritas$cloudRenderer = new VintageCloudRenderer(this.mc.getResourceManager());
        }

        this.celeritas$cloudRenderer.render(this.world, this.cloudTickCounter, partialTicks, pass, x, y, z);
    }

    @Inject(method = "onResourceManagerReload", at = @At("RETURN"))
    private void celeritas$reloadCloudTexture(IResourceManager resourceManager, CallbackInfo ci) {
        if (this.celeritas$cloudRenderer != null) {
            this.celeritas$cloudRenderer.reloadTextures(resourceManager);
        }
    }

    @Inject(method = "loadRenderers", at = @At("RETURN"))
    private void celeritas$destroyCloudRenderer(CallbackInfo ci) {
        // Will be re-allocated on next use
        if (this.celeritas$cloudRenderer != null) {
            this.celeritas$cloudRenderer.destroy();
            this.celeritas$cloudRenderer = null;
        }
    }
}
