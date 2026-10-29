package net.irisshaders.iris.mixin;

import net.irisshaders.iris.layer.GbufferPrograms;
import net.irisshaders.iris.pipeline.WorldRenderingPhase;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Begins the entities phase for submitted geometry. Much of it is only drawn once the buffer source's batches end,
 * after these return, so the phase is ended by whatever comes next.
 */
@Mixin(FeatureRenderDispatcher.class)
public class MixinFeatureRenderDispatcher {
	@Inject(method = {"renderSolidFeatures", "renderTranslucentFeatures"}, at = @At("HEAD"))
	private void iris$beginEntities(CallbackInfo ci) {
		GbufferPrograms.setPhase(WorldRenderingPhase.ENTITIES);
	}
}
