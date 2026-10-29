package net.irisshaders.iris.mixin.sky;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.layer.GbufferPrograms;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPhase;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.minecraft.client.renderer.SkyRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sets the phase for each part of the sky, lets pipelines hide the sun and moon, and tilts the sun's path.
 */
@Mixin(SkyRenderer.class)
public class MixinSkyRenderer_Phases {
	@Inject(method = "renderSkyDisc", at = @At("HEAD"))
	private void iris$beginSky(CallbackInfo ci) {
		GbufferPrograms.setPhase(WorldRenderingPhase.SKY);

		// Drawn before vanilla's sky, so that mods drawing over the sky still draw over it.
		if (Iris.getPipelineManager().getPipelineNullable() instanceof IrisRenderingPipeline pipeline) {
			pipeline.renderHorizon();
		}
	}

	@Inject(method = "renderSunriseAndSunset", at = @At("HEAD"))
	private void iris$beginSunset(CallbackInfo ci) {
		GbufferPrograms.setPhase(WorldRenderingPhase.SUNSET);
	}

	@Inject(method = "renderSun", at = @At("HEAD"), cancellable = true)
	private void iris$beginSun(CallbackInfo ci) {
		GbufferPrograms.setPhase(WorldRenderingPhase.SUN);

		WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();

		if (pipeline != null && !pipeline.shouldRenderSun()) {
			ci.cancel();
		}
	}

	@Inject(method = "renderMoon", at = @At("HEAD"), cancellable = true)
	private void iris$beginMoon(CallbackInfo ci) {
		GbufferPrograms.setPhase(WorldRenderingPhase.MOON);

		WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();

		if (pipeline != null && !pipeline.shouldRenderMoon()) {
			ci.cancel();
		}
	}

	@Inject(method = "renderStars", at = @At("HEAD"))
	private void iris$beginStars(CallbackInfo ci) {
		GbufferPrograms.setPhase(WorldRenderingPhase.STARS);
	}

	@Inject(method = "renderDarkDisc", at = @At("HEAD"))
	private void iris$beginVoid(CallbackInfo ci) {
		GbufferPrograms.setPhase(WorldRenderingPhase.VOID);
	}

	// Between vanilla's rotation that orients the sky and the ones that move the sun, moon and stars across it.
	@Inject(method = "renderSunMoonAndStars", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionfc;)V", ordinal = 0, shift = At.Shift.AFTER))
	private void iris$tiltSunPath(CallbackInfo ci, @Local(argsOnly = true) PoseStack poseStack) {
		WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();

		if (pipeline != null) {
			poseStack.mulPose(Axis.ZP.rotationDegrees(pipeline.getSunPathRotation()));
		}
	}
}
