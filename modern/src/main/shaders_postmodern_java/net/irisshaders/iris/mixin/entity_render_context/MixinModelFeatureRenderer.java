package net.irisshaders.iris.mixin.entity_render_context;

import com.llamalad7.mixinextras.sugar.Local;
import net.irisshaders.iris.layer.RenderContextIds;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelFeatureRenderer.class)
public class MixinModelFeatureRenderer {
	@Inject(method = "renderModel", at = @At("HEAD"))
	private void iris$restoreIds(CallbackInfo ci, @Local(argsOnly = true) SubmitNodeStorage.ModelSubmit<?> submit) {
		((RenderContextIds.Holder) (Object) submit).iris$restoreIds();
	}

	@Inject(method = {"renderSolid", "renderTranslucent"}, at = @At("RETURN"))
	private void iris$resetIds(CallbackInfo ci) {
		RenderContextIds.reset();
	}
}
