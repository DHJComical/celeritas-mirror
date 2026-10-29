package net.irisshaders.iris.mixin;

import net.irisshaders.iris.layer.GbufferPrograms;
import net.irisshaders.iris.pipeline.WorldRenderingPhase;
import net.minecraft.client.renderer.feature.ParticleFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Particles are drawn as they're rendered, between other features, so the phase is restored after them.
 */
@Mixin(ParticleFeatureRenderer.class)
public class MixinParticleFeatureRenderer {
	@Unique
	private WorldRenderingPhase iris$previousPhase = WorldRenderingPhase.NONE;

	@Inject(method = {"renderSolid", "renderTranslucent"}, at = @At("HEAD"))
	private void iris$beginParticles(CallbackInfo ci) {
		iris$previousPhase = GbufferPrograms.getCurrentPhase();
		GbufferPrograms.setPhase(WorldRenderingPhase.PARTICLES);
	}

	@Inject(method = {"renderSolid", "renderTranslucent"}, at = @At("RETURN"))
	private void iris$endParticles(CallbackInfo ci) {
		GbufferPrograms.setPhase(iris$previousPhase);
	}
}
