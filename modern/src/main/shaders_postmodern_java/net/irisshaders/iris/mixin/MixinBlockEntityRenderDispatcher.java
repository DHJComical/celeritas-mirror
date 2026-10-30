package net.irisshaders.iris.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.irisshaders.iris.layer.BlockEntityRenderType;
import net.irisshaders.iris.layer.RenderContextIds;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderDispatcher.class)
public class MixinBlockEntityRenderDispatcher {
	@Inject(method = "submit", at = @At("HEAD"))
	private void iris$beginBlockEntity(CallbackInfo ci, @Local(argsOnly = true) BlockEntityRenderState state) {
		BlockEntityRenderType.setSubmitting(true);
		RenderContextIds.setBlockEntity(state.blockState);
	}

	@Inject(method = "submit", at = @At("RETURN"))
	private void iris$endBlockEntity(CallbackInfo ci) {
		BlockEntityRenderType.setSubmitting(false);
		RenderContextIds.reset();
	}
}
