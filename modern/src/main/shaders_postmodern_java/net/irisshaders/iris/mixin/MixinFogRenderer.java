package net.irisshaders.iris.mixin;

import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.material.FogType;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public class MixinFogRenderer {
	@Inject(method = "setupFog", at = @At("RETURN"))
	private void iris$captureFog(Camera camera, int renderDistanceInChunks, DeltaTracker deltaTracker, float darkenWorldAmount,
								 ClientLevel level, CallbackInfoReturnable<FogData> cir) {
		if (camera.getFluidInCamera() == FogType.WATER) {
			float density = 0.05F;

			if (camera.entity() instanceof LocalPlayer localPlayer) {
				density -= localPlayer.getWaterVision() * localPlayer.getWaterVision() * 0.03F;

				// Swamps, as the water fog tag that used to mark them no longer exists.
				if (localPlayer.level().getBiome(localPlayer.blockPosition()).is(BiomeTags.HAS_RUINED_PORTAL_SWAMP)) {
					density += 0.005F;
				}
			}

			CapturedRenderingState.INSTANCE.setFogDensity(density);
		} else {
			CapturedRenderingState.INSTANCE.setFogDensity(-1.0F);
		}

		Vector4f color = cir.getReturnValue().color;
		CapturedRenderingState.INSTANCE.setFogColor(color.x, color.y, color.z);
	}
}
