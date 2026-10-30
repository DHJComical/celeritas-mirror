package net.irisshaders.iris.mixin.entity_render_context;

import net.irisshaders.iris.layer.RenderContextIds;
import net.minecraft.client.renderer.feature.NameTagFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NameTagFeatureRenderer.class)
public class MixinNameTagFeatureRenderer {
	@Inject(method = "renderTranslucent", at = @At("HEAD"))
	private void iris$setNameTagId(CallbackInfo ci) {
		RenderContextIds.reset();
		RenderContextIds.setEntity(RenderContextIds.NAME_TAG);
	}

	@Inject(method = "renderTranslucent", at = @At("RETURN"))
	private void iris$resetIds(CallbackInfo ci) {
		RenderContextIds.reset();
	}
}
