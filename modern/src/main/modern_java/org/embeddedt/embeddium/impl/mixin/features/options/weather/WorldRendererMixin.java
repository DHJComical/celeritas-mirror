package org.embeddedt.embeddium.impl.mixin.features.options.weather;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.embeddedt.embeddium.impl.Celeritas;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LevelRenderer.class)
public class WorldRendererMixin {
    //? if >=1.16 {
    @Redirect(method =
            "renderSnowAndRain"
            , at = @At(value = "INVOKE", target ="Lnet/minecraft/client/Minecraft;useFancyGraphics()Z"))
    private boolean redirectGetFancyWeather() {
        return Celeritas.options().quality.weatherQuality.isFancy(Celeritas.areGraphicsFancy());
    }
    //?} else {
    /*@ModifyExpressionValue(method = "renderSnowAndRain", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;fancyGraphics:Z"))
    private boolean redirectGetFancyWeather(boolean isFancy) {
        return Celeritas.options().quality.weatherQuality.isFancy(isFancy);
    }
    *///?}
}