package net.irisshaders.iris.mixin.vertices;

import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.minecraft.world.level.CardinalLighting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Allows the vanilla directional shading effect to be fully disabled by shader packs, which implement their own
 * lighting. Celeritas shades terrain itself; this covers blocks vanilla tesselates, such as moving blocks.
 */
@Mixin(CardinalLighting.class)
public class MixinCardinalLighting {
	@Inject(method = "byFace", at = @At("HEAD"), cancellable = true)
	private void iris$maybeDisableDirectionalShading(CallbackInfoReturnable<Float> cir) {
		if (WorldRenderingSettings.INSTANCE.shouldDisableDirectionalShading()) {
			cir.setReturnValue(1.0F);
		}
	}
}
