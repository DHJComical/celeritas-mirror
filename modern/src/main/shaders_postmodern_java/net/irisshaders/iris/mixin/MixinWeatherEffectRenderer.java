package net.irisshaders.iris.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.layer.GbufferPrograms;
import net.irisshaders.iris.pipeline.WorldRenderingPhase;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WeatherEffectRenderer.class)
public class MixinWeatherEffectRenderer {
	private static final String RENDER = "render(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/state/level/WeatherRenderState;Lnet/minecraft/client/renderer/state/level/LevelRenderState;)V";

	@Inject(method = RENDER, at = @At("HEAD"))
	private void iris$beginWeather(CallbackInfo ci) {
		GbufferPrograms.setPhase(WorldRenderingPhase.RAIN_SNOW);
	}

	@Inject(method = RENDER, at = @At("RETURN"))
	private void iris$endWeather(CallbackInfo ci) {
		GbufferPrograms.setPhase(WorldRenderingPhase.NONE);
	}

	// Vanilla only writes weather to the depth buffer for Fabulous graphics, which shaders replace.
	@ModifyExpressionValue(method = RENDER, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;useShaderTransparency()Z"))
	private boolean iris$writeWeatherDepth(boolean useShaderTransparency) {
		WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();

		return useShaderTransparency || (pipeline != null && pipeline.shouldWriteRainAndSnowToDepthBuffer());
	}
}
