package net.irisshaders.iris.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.irisshaders.iris.pathways.scaling.RenderScale;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Redirects {@link Minecraft#getMainRenderTarget()} to the reduced-resolution world target
 * while {@link RenderScale} is active, so the whole shader pipeline renders at that resolution.
 *
 * <p>Minecraft's own bind/unbind/blit in {@code runTick} touches the {@code mainRenderTarget}
 * field directly, so the GUI and final present stay at native resolution. {@code resizeDisplay}
 * does go through the getter, so the redirect is suspended around it to avoid resizing our
 * scaled target and leaving Minecraft's own stale.</p>
 */
@Mixin(Minecraft.class)
public class MixinMinecraft_RenderScale {
	@ModifyReturnValue(method = "getMainRenderTarget", at = @At("RETURN"))
	private RenderTarget celeritas$redirectToScaledWorldTarget(RenderTarget original) {
		return RenderScale.isActive() ? RenderScale.getWorldTarget() : original;
	}

	@Inject(method = "resizeDisplay", at = @At("HEAD"))
	private void celeritas$beginResizeDisplay(CallbackInfo ci, @Share("wasActive") LocalBooleanRef wasActive) {
		wasActive.set(RenderScale.isActive());
		RenderScale.setActive(false);
	}

	@Inject(method = "resizeDisplay", at = @At("RETURN"))
	private void celeritas$endResizeDisplay(CallbackInfo ci, @Share("wasActive") LocalBooleanRef wasActive) {
		RenderScale.setActive(wasActive.get());
	}
}
