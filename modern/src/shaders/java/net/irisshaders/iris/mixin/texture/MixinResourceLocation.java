package net.irisshaders.iris.mixin.texture;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ResourceLocation.class, priority = 1010)
public class MixinResourceLocation {
	@ModifyReturnValue(method = "isValidPath", at = @At("RETURN"))
	private static boolean iris$blockDUMMY(boolean original, String string) {
        return original && !string.equals("DUMMY");
    }

	@ModifyReturnValue(method = "validPathChar", at = @At("RETURN"))
	private static boolean iris$allowInvalidPaths(boolean original, char c) {
        return original || (c >= 'A' && c <= 'Z');
    }
}
