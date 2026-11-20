package org.embeddedt.embeddium.impl.mixin.features.render.world.sky;

import net.minecraft.client.renderer.LevelRenderer;
import org.embeddedt.embeddium.impl.gl.compat.FogHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class SkyFogLevelRendererMixin {
    @Inject(method = "renderLevel", at = { @At("HEAD"), @At("RETURN") })
    private void celeritas$invalidateSkyTexture(CallbackInfo ci) {
        FogHelper.INSTANCE.invalidateSkyTexture();
    }

    @Inject(method = "renderSky", at = @At("RETURN"))
    private void celeritas$captureSkyTexture(CallbackInfo ci) {
        FogHelper.INSTANCE.captureSkyTexture();
    }
}
