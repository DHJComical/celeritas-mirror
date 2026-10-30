package net.irisshaders.iris.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.irisshaders.iris.pathways.HandRenderer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Leaves the hands submitted for Iris's hand renderer to draw, rather than drawing everything submitted to vanilla's.
 */
@Mixin(ItemInHandRenderer.class)
public class MixinItemInHandRenderer {
	@WrapWithCondition(method = "renderHandsWithItems", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;renderAllFeatures()V"))
	private boolean iris$skipDrawingFeatures(FeatureRenderDispatcher dispatcher) {
		return !HandRenderer.INSTANCE.isSubmitting();
	}

	@WrapWithCondition(method = "renderHandsWithItems", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;endBatch()V"))
	private boolean iris$skipEndingBatch(MultiBufferSource.BufferSource bufferSource) {
		return !HandRenderer.INSTANCE.isSubmitting();
	}
}
