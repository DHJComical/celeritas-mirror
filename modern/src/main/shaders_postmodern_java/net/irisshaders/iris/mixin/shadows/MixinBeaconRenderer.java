package net.irisshaders.iris.mixin.shadows;

import net.irisshaders.iris.shadows.ShadowRenderingState;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BeaconRenderer.class)
public class MixinBeaconRenderer {
	// Beams would otherwise cast a shadow the height of the world.
	@Inject(method = "submit(Lnet/minecraft/client/renderer/blockentity/state/BeaconRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("HEAD"), cancellable = true)
	private void iris$noBeamInShadowPass(CallbackInfo ci) {
		if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
			ci.cancel();
		}
	}
}
