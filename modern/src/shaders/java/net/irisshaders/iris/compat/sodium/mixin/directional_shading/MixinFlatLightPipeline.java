package net.irisshaders.iris.compat.sodium.mixin.directional_shading;

import org.embeddedt.embeddium.impl.model.light.flat.FlatLightPipeline;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FlatLightPipeline.class)
public class MixinFlatLightPipeline {
	@Inject(method = "calculate", at = @At(value = "INVOKE", target = "Ljava/util/Arrays;fill([II)V", ordinal = 0, shift = At.Shift.AFTER), cancellable = true, remap = false)
	private void iris$disableDirectionalShading(CallbackInfo ci) {
		if (WorldRenderingSettings.INSTANCE.shouldDisableDirectionalShading()) {
			ci.cancel();
		}
	}
}
