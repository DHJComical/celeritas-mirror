package net.irisshaders.iris.compat.sodium.mixin.options;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.embeddedt.embeddium.impl.render.chunk.RenderSectionManager;
import net.irisshaders.iris.Iris;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Disables fog occlusion when a shader pack is enabled, since shaders are not guaranteed to actually implement fog.
 */
@Mixin(RenderSectionManager.class)
public class MixinRenderSectionManager {
	@ModifyExpressionValue(method = "getSearchDistance", remap = false,
		at = @At(value = "INVOKE",
			target = "Lorg/embeddedt/embeddium/impl/render/chunk/RenderSectionManager;useFogOcclusion()Z",
			remap = false))
	private boolean iris$disableFogOcclusion(boolean original) {
		if (Iris.getCurrentPack().isPresent()) {
			return false;
		} else {
			return original;
		}
	}
}
