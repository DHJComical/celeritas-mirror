package net.irisshaders.iris.mixin.gui;

import net.irisshaders.iris.Iris;
import net.irisshaders.iris.gui.screen.HudHideable;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class MixinGui {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
	private void iris$handleHudHidingScreens(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		if (this.minecraft.screen instanceof HudHideable) {
			ci.cancel();
		}
	}

	@Inject(method = "extractVignette", at = @At("HEAD"), cancellable = true)
	private void iris$disableVignetteRendering(GuiGraphicsExtractor graphics, @Nullable Entity camera, CallbackInfo ci) {
		WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();

		if (pipeline != null && !pipeline.shouldRenderVignette()) {
			ci.cancel();
		}
	}
}
