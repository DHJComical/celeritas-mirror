package net.irisshaders.iris.mixin.gui;

import static com.mitchej123.glsm.RenderSystemService.RENDER_SYSTEM;

import com.mojang.blaze3d.systems.RenderSystem;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.gui.screen.HudHideable;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
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

	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	public void iris$handleHudHidingScreens(CallbackInfo ci) {
		Screen screen = this.minecraft.screen;

		if (screen instanceof HudHideable) {
			ci.cancel();
		}
	}

	@Inject(method = "renderVignette", at = @At("HEAD"), cancellable = true)
	private void iris$disableVignetteRendering(CallbackInfo ci) {
		WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();

		if (pipeline != null && !pipeline.shouldRenderVignette()) {
			// we need to set up the GUI render state ourselves if we cancel the vignette
			RENDER_SYSTEM.enableDepthTest();
			RenderSystem.defaultBlendFunc();

			ci.cancel();
		}
	}
}
