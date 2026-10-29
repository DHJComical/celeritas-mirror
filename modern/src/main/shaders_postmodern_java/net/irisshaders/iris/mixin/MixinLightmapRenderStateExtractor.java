package net.irisshaders.iris.mixin;

import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LightmapRenderStateExtractor.class)
public class MixinLightmapRenderStateExtractor {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Inject(method = "extract", at = @At("HEAD"))
	private void iris$resetDarknessValue(LightmapRenderState renderState, float partialTicks, CallbackInfo ci) {
		CapturedRenderingState.INSTANCE.setDarknessLightFactor(0.0F);
	}

	@Inject(method = "calculateDarknessScale", at = @At("RETURN"))
	private void iris$storeDarknessValue(LivingEntity camera, float darknessGamma, float partialTickTime, CallbackInfoReturnable<Float> cir) {
		CapturedRenderingState.INSTANCE.setDarknessLightFactor((float) (cir.getReturnValue() * this.minecraft.options.darknessEffectScale().get()));
	}
}
