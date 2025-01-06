package net.irisshaders.iris.compat.sodium.mixin.directional_shading;

import com.llamalad7.mixinextras.sugar.Local;
import org.embeddedt.embeddium.impl.model.light.data.QuadLightData;
import org.embeddedt.embeddium.impl.model.light.flat.FlatLightPipeline;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;

@Mixin(FlatLightPipeline.class)
public class MixinFlatLightPipeline {
	@Inject(method = "calculate", at = @At(value = "RETURN"))
	private void iris$disableDirectionalShading(CallbackInfo ci, @Local(ordinal = 0, argsOnly = true) QuadLightData out) {
		if (WorldRenderingSettings.INSTANCE.shouldDisableDirectionalShading()) {
            Arrays.fill(out.br, 1.0f);
		}
	}
}
