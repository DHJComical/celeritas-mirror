package net.irisshaders.iris.mixin.shadows;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.irisshaders.iris.shadows.ShadowRenderingState;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BeaconRenderer.class)
public class MixinBeaconRenderer {
	@ModifyExpressionValue(method = "render*",
		at = @At(value = "INVOKE", target = "Ljava/util/List;size()I"))
	private static int iris$noLightBeamInShadowPass(int size) {
		if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
			// TODO: Don't do this if we're doing the "Unified Entity Rendering" optimization
			// TODO: This isn't necessary on most shaderpacks if we support blockEntityId
			return 0;
		}
        return size;
	}
}
