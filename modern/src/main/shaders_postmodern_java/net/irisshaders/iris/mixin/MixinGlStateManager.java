package net.irisshaders.iris.mixin;

import com.mojang.blaze3d.opengl.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(GlStateManager.class)
public class MixinGlStateManager {
	@ModifyConstant(method = "<clinit>", constant = @Constant(intValue = 12), require = 1)
	private static int iris$increaseMaximumAllowedTextureUnits(int existingValue) {
		// Iris binds more texture units than vanilla tracks, and OpenGL can't be queried this early.
		return 128;
	}
}
