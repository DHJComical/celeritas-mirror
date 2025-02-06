package net.irisshaders.iris.compat.sodium.mixin.shadow_map;

import net.minecraft.client.renderer.entity.EntityRenderer;
import org.embeddedt.embeddium.impl.modern.render.chunk.ModernRenderSectionManager;
import org.embeddedt.embeddium.impl.render.CeleritasWorldRenderer;
import net.irisshaders.iris.shadows.ShadowRenderingState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Ensures that the state of the chunk render visibility graph gets properly swapped when in the shadow map pass,
 * because we must maintain one visibility graph for the shadow camera and one visibility graph for the player camera.
 * <p>
 * Also ensures that the visibility graph is always rebuilt in the shadow pass, since the shadow camera is generally
 * always moving.
 */
@Mixin(CeleritasWorldRenderer.class)
public abstract class MixinSodiumWorldRenderer {

	@Inject(method = "isEntityVisible", at = @At("HEAD"), cancellable = true)
	private void iris$overrideEntityCulling(Entity entity, EntityRenderer renderer, CallbackInfoReturnable<Boolean> cir) {
		if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) cir.setReturnValue(true);
	}

	@Redirect(method = "setupTerrain",
		at = @At(value = "INVOKE",
			target = "Lorg/embeddedt/embeddium/impl/modern/render/chunk/ModernRenderSectionManager;needsUpdate()Z",
			remap = false))
	private boolean iris$forceChunkGraphRebuildInShadowPass(ModernRenderSectionManager instance) {
		if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
			// TODO: Detect when the sun/moon isn't moving
			return true;
		} else {
			return instance.needsUpdate();
		}
	}
}
