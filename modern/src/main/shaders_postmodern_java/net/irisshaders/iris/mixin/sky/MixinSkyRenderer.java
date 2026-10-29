package net.irisshaders.iris.mixin.sky;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Disables the sunrise / sunset effect when blindness is active or when submerged in a fluid.
 */
@Mixin(SkyRenderer.class)
public class MixinSkyRenderer {
	@Inject(method = "renderSunriseAndSunset", at = @At("HEAD"), cancellable = true)
	private void iris$disableSunriseInFog(PoseStack poseStack, float sunAngle, int sunriseAndSunsetColor, CallbackInfo ci) {
		Entity cameraEntity = Minecraft.getInstance().getCameraEntity();

		if (cameraEntity instanceof LivingEntity living && living.hasEffect(MobEffects.BLINDNESS)) {
			ci.cancel();
		} else if (Minecraft.getInstance().gameRenderer.getMainCamera().getFluidInCamera() != FogType.NONE) {
			ci.cancel();
		}
	}
}
