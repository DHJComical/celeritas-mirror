package org.taumc.celeritas.mixin.core;

import net.minecraft.client.option.GameOptions;
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

    @Inject(method = "updateSkyAndFogColors", at = @At("RETURN"))
    private void captureFogColor(float par1, CallbackInfo ci) {
        GLStateManagerFogService.fogColorRed = this.fogRed;
        GLStateManagerFogService.fogColorGreen = this.fogGreen;
        GLStateManagerFogService.fogColorBlue = this.fogBlue;
    }

    @Redirect(method = "renderFrame", at = @At(value = "FIELD", target = "Lnet/minecraft/client/option/GameOptions;fancyGraphics:Z"))
    private boolean celeritas$forceNormalTranslucentTerrainRendering(GameOptions instance) {
        return false;
    }
}
