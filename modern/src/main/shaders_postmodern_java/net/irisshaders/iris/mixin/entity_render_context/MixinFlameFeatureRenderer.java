package net.irisshaders.iris.mixin.entity_render_context;

import net.irisshaders.iris.layer.RenderContextIds;
import net.minecraft.client.renderer.feature.FlameFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FlameFeatureRenderer.class)
public class MixinFlameFeatureRenderer {
	@Inject(method = "renderSolid", at = @At("HEAD"))
	private void iris$setFlameId(CallbackInfo ci) {
		RenderContextIds.reset();
		RenderContextIds.setEntity(RenderContextIds.ENTITY_FLAME);
	}

	@Inject(method = "renderSolid", at = @At("RETURN"))
	private void iris$resetIds(CallbackInfo ci) {
		RenderContextIds.reset();
	}
}
