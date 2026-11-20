package org.taumc.celeritas.mixin.features.render.sky;

import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.taumc.celeritas.impl.render.terrain.fog.GLStateManagerFogService;

@Mixin(EntityRenderer.class)
public class SkyFogEntityRendererMixin {
    @Inject(method = "renderWorldPass", at = { @At("HEAD"), @At("RETURN") })
    private void celeritas$invalidateSkyTexture(CallbackInfo ci) {
        GLStateManagerFogService.INSTANCE.invalidateSkyTexture();
    }
}
