package org.embeddedt.embeddium.impl.mixin.features.options.weather;

//? if >=1.21.2
/*import net.minecraft.client.renderer.WeatherEffectRenderer;*/
import org.embeddedt.embeddium.impl.SodiumClientMod;
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
    @Redirect(method =
            //? if >=1.21.2
            /*"render(Lnet/minecraft/world/level/Level;Lnet/minecraft/client/renderer/LightTexture;IFLnet/minecraft/world/phys/Vec3;)V"*/
            //? if <1.21.2
            "renderSnowAndRain"
            , at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;useFancyGraphics()Z"))
    private boolean redirectGetFancyWeather() {
        return SodiumClientMod.options().quality.weatherQuality.isFancy(Minecraft.getInstance().options.graphicsMode().get());
    }
}