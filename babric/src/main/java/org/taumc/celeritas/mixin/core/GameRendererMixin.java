package org.taumc.celeritas.mixin.core;

import net.minecraft.client.options.GameOptions;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.taumc.celeritas.impl.render.terrain.fog.GLStateManagerFogService;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Shadow
    private float fogRed;

    @Shadow
    private float fogGreen;

    @Shadow
    private float fogBlue;

    @Inject(method = "setupFog", at = @At("RETURN"))
    private void captureFogColor(float par1, CallbackInfo ci) {
        GLStateManagerFogService.fogColorRed = this.fogRed;
        GLStateManagerFogService.fogColorGreen = this.fogGreen;
        GLStateManagerFogService.fogColorBlue = this.fogBlue;
    }

    @Redirect(method = "renderWorld", at = @At(value = "FIELD", target = "Lnet/minecraft/client/options/GameOptions;fancyGraphics:Z"))
    private boolean celeritas$forceNormalTranslucentTerrainRendering(GameOptions instance) {
        return false;
    }
}
