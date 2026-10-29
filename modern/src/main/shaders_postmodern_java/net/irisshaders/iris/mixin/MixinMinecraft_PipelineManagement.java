package net.irisshaders.iris.mixin;

import net.irisshaders.iris.Iris;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.taumc.celeritas.shaders.CeleritasShaders;

@Mixin(Minecraft.class)
public class MixinMinecraft_PipelineManagement {
	/**
	 * Runs before the level field is cleared when disconnecting.
	 */
	@Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V", at = @At("HEAD"))
	private void iris$trackLastDimensionOnLeave(CallbackInfo ci) {
		Iris.lastDimension = Iris.getCurrentDimension();
	}

	/**
	 * Runs before the level field is replaced after receiving a login or respawn packet.
	 */
	@Inject(method = "setLevel", at = @At("HEAD"))
	private void iris$trackLastDimensionOnLevelChange(CallbackInfo ci) {
		Iris.lastDimension = Iris.getCurrentDimension();
	}

	/**
	 * Recreates the pipeline when the dimension changes, before the level renderer receives the new level, so that
	 * the terrain renderer is set up for the new pipeline's vertex format.
	 */
	@Inject(method = "updateLevelInEngines(Lnet/minecraft/client/multiplayer/ClientLevel;Z)V", at = @At("HEAD"))
	private void iris$resetPipeline(@Nullable ClientLevel level, boolean stopSound, CallbackInfo ci) {
		if (Iris.getCurrentDimension() != Iris.lastDimension) {
			CeleritasShaders.logger().info("Reloading pipeline on dimension change: " + Iris.lastDimension + " => " + Iris.getCurrentDimension());
			Iris.getPipelineManager().destroyPipeline();

			if (level != null) {
				Iris.getPipelineManager().preparePipeline(Iris.getCurrentDimension());
			}
		}
	}
}
