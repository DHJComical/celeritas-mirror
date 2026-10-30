package net.irisshaders.iris.mixin.entity_render_context;

import net.irisshaders.iris.layer.RenderContextIds;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemFeatureRenderer.class)
public class MixinItemFeatureRenderer {
	@Inject(method = "renderItem", at = @At("HEAD"))
	private void iris$restoreIds(MultiBufferSource.BufferSource bufferSource, OutlineBufferSource outlineBufferSource,
								 SubmitNodeStorage.ItemSubmit submit, CallbackInfo ci) {
		((RenderContextIds.Holder) (Object) submit).iris$restoreIds();
	}

	@Inject(method = {"renderSolid", "renderTranslucent"}, at = @At("RETURN"))
	private void iris$resetIds(CallbackInfo ci) {
		RenderContextIds.reset();
	}
}
