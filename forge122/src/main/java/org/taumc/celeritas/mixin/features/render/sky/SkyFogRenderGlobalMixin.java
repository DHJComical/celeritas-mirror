package org.taumc.celeritas.mixin.features.render.sky;

import net.minecraft.client.renderer.RenderGlobal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.taumc.celeritas.impl.render.terrain.fog.GLStateManagerFogService;

@Mixin(RenderGlobal.class)
public class SkyFogRenderGlobalMixin {
    @Inject(method = "renderSky(FI)V", at = @At("RETURN"))
    private void celeritas$captureSkyTexture(CallbackInfo ci) {
        GLStateManagerFogService.INSTANCE.captureSkyTexture();
    }
}
