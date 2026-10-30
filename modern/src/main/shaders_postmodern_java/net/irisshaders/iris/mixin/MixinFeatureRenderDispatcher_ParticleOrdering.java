package net.irisshaders.iris.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.irisshaders.iris.pathways.ParticleOrdering;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.feature.ParticleFeatureRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FeatureRenderDispatcher.class)
public class MixinFeatureRenderDispatcher_ParticleOrdering implements ParticleOrdering {
	@Shadow
	@Final
	private SubmitNodeStorage submitNodeStorage;

	@Shadow
	@Final
	private ParticleFeatureRenderer particleFeatureRenderer;

	@Unique
	private boolean iris$deferSolidParticles;

	@WrapWithCondition(method = "renderSolidFeatures", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/ParticleFeatureRenderer;renderSolid(Lnet/minecraft/client/renderer/SubmitNodeCollection;)V"))
	private boolean iris$deferSolidParticles(ParticleFeatureRenderer renderer, SubmitNodeCollection collection) {
		return !this.iris$deferSolidParticles;
	}

	@Override
	public void iris$setDeferSolidParticles(boolean defer) {
		this.iris$deferSolidParticles = defer;
	}

	@Override
	public void iris$renderSolidParticles() {
		for (SubmitNodeCollection collection : this.submitNodeStorage.getSubmitsPerOrder().values()) {
			this.particleFeatureRenderer.renderSolid(collection);
		}
	}
}
