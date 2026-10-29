package net.irisshaders.iris.mixin;

import net.irisshaders.iris.layer.BlockEntityRenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderDispatcher.class)
public class MixinBlockEntityRenderDispatcher {
	@Inject(method = "submit", at = @At("HEAD"))
	private void iris$beginBlockEntity(CallbackInfo ci) {
		BlockEntityRenderType.setSubmitting(true);
	}

	@Inject(method = "submit", at = @At("RETURN"))
	private void iris$endBlockEntity(CallbackInfo ci) {
		BlockEntityRenderType.setSubmitting(false);
	}
}
