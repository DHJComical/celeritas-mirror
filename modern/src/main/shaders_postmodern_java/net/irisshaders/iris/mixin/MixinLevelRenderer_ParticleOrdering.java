package net.irisshaders.iris.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.irisshaders.iris.pathways.ParticleOrdering;
import net.irisshaders.iris.shaderpack.properties.ParticleRenderingSettings;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws all particles before translucent terrain for {@link ParticleRenderingSettings#BEFORE}, or all after it for
 * {@link ParticleRenderingSettings#AFTER}.
 */
@Mixin(LevelRenderer.class)
public class MixinLevelRenderer_ParticleOrdering {
	@Shadow
	@Final
	private FeatureRenderDispatcher featureRenderDispatcher;

	@WrapOperation(method = "lambda$addMainPass$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;renderSolidFeatures()V"))
	private void iris$deferSolidParticles(FeatureRenderDispatcher dispatcher, Operation<Void> original) {
		boolean defer = ParticleOrdering.current() == ParticleRenderingSettings.AFTER;
		((ParticleOrdering) dispatcher).iris$setDeferSolidParticles(defer);

		try {
			original.call(dispatcher);
		} finally {
			((ParticleOrdering) dispatcher).iris$setDeferSolidParticles(false);
		}
	}

	@Inject(method = "lambda$addMainPass$0", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;push(Ljava/lang/String;)V", args = "ldc=translucentTerrain"))
	private void iris$renderParticlesBeforeTranslucentTerrain(CallbackInfo ci) {
		if (ParticleOrdering.current() == ParticleRenderingSettings.BEFORE) {
			this.featureRenderDispatcher.renderTranslucentParticles();
		}
	}

	@WrapOperation(method = "lambda$addMainPass$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;renderTranslucentParticles()V"))
	private void iris$renderParticlesAfterTranslucentTerrain(FeatureRenderDispatcher dispatcher, Operation<Void> original) {
		switch (ParticleOrdering.current()) {
			case BEFORE -> {
			}
			case AFTER -> {
				((ParticleOrdering) dispatcher).iris$renderSolidParticles();
				original.call(dispatcher);
			}
			case MIXED -> original.call(dispatcher);
		}
	}
}
