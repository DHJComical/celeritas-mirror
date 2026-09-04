package net.irisshaders.iris.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.irisshaders.iris.pathways.scaling.RenderScale;
import net.irisshaders.iris.shadows.ShadowRenderingState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Makes {@code bindWrite(false)} set the viewport while {@link RenderScale} is active.
 *
 * <p>{@code bindWrite(false)} means "bind for writing, the viewport is already correct" - true
 * during normal world rendering since every render target bound there matches the main render
 * target's size. Render scaling breaks that: the world target is smaller than the window, while
 * the glowing-effect target (and, under Fabulous graphics, the item entity and weather targets)
 * stay at window size.</p>
 *
 * <p>Concretely, {@code LevelRenderer.renderLevel} clears the entity outline target (which
 * binds it with a correct viewport), then calls {@code getMainRenderTarget().bindWrite(false)}
 * without touching the viewport. Everything rendered until the deferred passes reset it -
 * entities, block entities, block breaking progress, the block hit outline - is rasterized with
 * a window-sized viewport into the smaller world target and lands in the wrong place on screen.
 * {@code RenderStateShard}'s outline output state has the same problem in reverse.</p>
 *
 * <p>Fixing it here rather than at each call site keeps it correct for mod-added cases too,
 * since {@code bindWrite(false)} is the idiomatic way to switch render targets mid-frame.</p>
 */
@Mixin(RenderTarget.class)
public class MixinRenderTarget_RenderScale {
	@Inject(method = "bindWrite(Z)V", at = @At("RETURN"))
	private void iris$restoreScaledViewport(boolean setViewport, CallbackInfo ci) {
		if (setViewport || !RenderScale.isActive()) {
			// bindWrite(true) has already set the viewport for us.
			return;
		}

		// The shadow pass renders into the shadow map at its own resolution and restores the
		// viewport itself afterwards. Iris rebinds the main render target with bindWrite(false)
		// while swapping shaders in there, so leave the viewport alone until it is done.
		if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
			return;
		}

		RenderTarget target = (RenderTarget) (Object) this;

		RenderSystem.viewport(0, 0, target.viewWidth, target.viewHeight);
	}
}
