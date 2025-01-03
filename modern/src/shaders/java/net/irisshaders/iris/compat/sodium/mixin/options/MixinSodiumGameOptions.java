package net.irisshaders.iris.compat.sodium.mixin.options;

import org.embeddedt.embeddium.impl.gui.SodiumGameOptions;
import net.irisshaders.iris.Iris;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;

/**
 * Ensures that the Iris config file is written whenever Sodium options are changed, in case the user changed the
 * Max Shadow Distance setting.
 */
@Mixin(SodiumGameOptions.class)
public class MixinSodiumGameOptions {
	@Inject(method = "writeToDisk", at = @At("RETURN"))
	private static void iris$writeIrisConfig(CallbackInfo ci) {
		try {
			if (Iris.getIrisConfig() != null) {
				Iris.getIrisConfig().save();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
