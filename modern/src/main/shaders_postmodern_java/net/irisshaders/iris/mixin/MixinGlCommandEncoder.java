package net.irisshaders.iris.mixin;

import com.mojang.blaze3d.textures.GpuTextureView;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Supplier;

/**
 * Redirects vanilla's draws into the shader pack's gbuffers while the world renders, by rebinding the framebuffer
 * once vanilla has set up each draw.
 */
@Mixin(targets = "com/mojang/blaze3d/opengl/GlCommandEncoder")
public class MixinGlCommandEncoder {
	@Inject(method = "createRenderPass(Ljava/util/function/Supplier;Lcom/mojang/blaze3d/textures/GpuTextureView;Ljava/util/OptionalInt;Lcom/mojang/blaze3d/textures/GpuTextureView;Ljava/util/OptionalDouble;)Lcom/mojang/blaze3d/systems/RenderPassBackend;", at = @At("HEAD"))
	private void iris$trackMainTarget(Supplier<String> label, GpuTextureView colorTexture, OptionalInt clearColor,
									  @Nullable GpuTextureView depthTexture, OptionalDouble clearDepth, CallbackInfoReturnable<?> cir) {
		WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();

		if (pipeline != null) {
			pipeline.getRenderTargetStateListener().setIsMainBound(colorTexture.texture() == Minecraft.getInstance().getMainRenderTarget().getColorTexture());
		}
	}

	@Inject(method = "trySetup", at = @At("RETURN"))
	private void iris$bindGbuffers(CallbackInfoReturnable<Boolean> cir) {
		// Vanilla binds sampler objects to its texture units for each draw and leaves them bound.
		IrisRenderSystem.invalidateSamplerBindings();

		if (cir.getReturnValueZ() && Iris.getPipelineManager().getPipelineNullable() instanceof IrisRenderingPipeline pipeline
			&& pipeline.shouldOverrideShaders()) {
			pipeline.bindDefault();
		}
	}
}
