package org.embeddedt.embeddium.impl.mixin.features.options.weather;

//? if >=1.21.2
/*import net.minecraft.client.renderer.WeatherEffectRenderer;*/
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Options;
import org.embeddedt.embeddium.impl.Celeritas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

//? if <1.21.2
@Mixin(LevelRenderer.class)
//? if >=1.21.2
/*@Mixin(WeatherEffectRenderer.class)*/
public class WorldRendererMixin {
    //? if >=1.16 {
    @Redirect(method =
            //? if >=1.21.2
            /*"*"*/
            //? if <1.21.2
            "renderSnowAndRain"
            , at = @At(value = "INVOKE", target ="Lnet/minecraft/client/Minecraft;useFancyGraphics()Z"))
    private boolean redirectGetFancyWeather() {
        return Celeritas.options().quality.weatherQuality.isFancy();
    }
    //?} else {
    /*@ModifyExpressionValue(method = "renderSnowAndRain", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;fancyGraphics:Z"))
    private boolean redirectGetFancyWeather(boolean isFancy) {
        return Celeritas.options().quality.weatherQuality.isFancy(isFancy);
    }
    *///?}
}