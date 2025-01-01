package net.irisshaders.iris.compat.sodium.mixin.shadow_map;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.embeddedt.embeddium.impl.render.chunk.DefaultChunkRenderer;
import net.irisshaders.iris.shadows.ShadowRenderingState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DefaultChunkRenderer.class)
public class MixinDefaultChunkRenderer {
	@ModifyExpressionValue(method = "render", at = @At(value = "FIELD", target = "Lorg/embeddedt/embeddium/impl/render/chunk/DefaultChunkRenderer;useBlockFaceCulling:Z"), remap = false)
	private boolean iris$disableBlockFaceCullingInShadowPass(boolean original) {
		if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) return false;
		return original;
	}
}
