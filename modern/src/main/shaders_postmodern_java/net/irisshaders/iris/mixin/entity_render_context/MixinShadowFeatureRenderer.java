package net.irisshaders.iris.mixin.entity_render_context;

import net.irisshaders.iris.layer.RenderContextIds;
import net.minecraft.client.renderer.feature.ShadowFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShadowFeatureRenderer.class)
public class MixinShadowFeatureRenderer {
	@Inject(method = "renderTranslucent", at = @At("HEAD"))
	private void iris$setShadowId(CallbackInfo ci) {
		RenderContextIds.reset();
		RenderContextIds.setEntity(RenderContextIds.ENTITY_SHADOW);
	}

	@Inject(method = "renderTranslucent", at = @At("RETURN"))
	private void iris$resetIds(CallbackInfo ci) {
		RenderContextIds.reset();
	}
}
