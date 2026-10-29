package net.irisshaders.iris.mixin;

import net.irisshaders.iris.layer.GbufferPrograms;
import net.irisshaders.iris.pipeline.WorldRenderingPhase;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.client.renderer.WorldBorderRenderer;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sets the phase for the renderers that draw as they render. Debug gizmos are only drawn once the buffer source's
 * batches end, so their phase is ended by whatever comes next.
 */
@Mixin({CloudRenderer.class, WorldBorderRenderer.class, DrawableGizmoPrimitives.class})
public class MixinWorldPhases {
	@Inject(method = "render", at = @At("HEAD"))
	private void iris$beginPhase(CallbackInfo ci) {
		Object self = this;

		GbufferPrograms.setPhase(switch (self) {
			case CloudRenderer ignored -> WorldRenderingPhase.CLOUDS;
			case WorldBorderRenderer ignored -> WorldRenderingPhase.WORLD_BORDER;
			default -> WorldRenderingPhase.DEBUG;
		});
	}

	@Inject(method = "render", at = @At("RETURN"))
	private void iris$endPhase(CallbackInfo ci) {
		if (!((Object) this instanceof DrawableGizmoPrimitives)) {
			GbufferPrograms.setPhase(WorldRenderingPhase.NONE);
		}
	}
}
