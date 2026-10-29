package net.irisshaders.iris.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.opengl.GlCommandEncoder;
import com.mojang.blaze3d.opengl.GlRenderPass;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.pipeline.programs.GbufferProgramBinder;
import net.irisshaders.iris.vertices.ImmediateState;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL40C;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Supplier;

/**
 * Redirects vanilla's draws into the shader pack's gbuffers while the world renders, by swapping in the pack's programs
 * and framebuffers once vanilla has set up each draw.
 */
@Mixin(GlCommandEncoder.class)
public class MixinGlCommandEncoder {
	@Inject(method = "createRenderPass(Ljava/util/function/Supplier;Lcom/mojang/blaze3d/textures/GpuTextureView;Ljava/util/OptionalInt;Lcom/mojang/blaze3d/textures/GpuTextureView;Ljava/util/OptionalDouble;)Lcom/mojang/blaze3d/systems/RenderPassBackend;", at = @At("HEAD"))
	private void iris$trackMainTarget(Supplier<String> label, GpuTextureView colorTexture, OptionalInt clearColor,
									  @Nullable GpuTextureView depthTexture, OptionalDouble clearDepth, CallbackInfoReturnable<?> cir) {
		WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();

		if (pipeline != null) {
			pipeline.getRenderTargetStateListener().setIsMainBound(colorTexture.texture() == Minecraft.getInstance().getMainRenderTarget().getColorTexture());
		}
	}

	@Inject(method = "trySetup", at = @At("HEAD"))
	private void iris$chooseGbufferProgram(GlRenderPass renderPass, Collection<String> dynamicUniforms, CallbackInfoReturnable<Boolean> cir) {
		GbufferProgramBinder.beforeSetup(renderPass);
	}

	@Inject(method = "trySetup", at = @At("RETURN"))
	private void iris$bindGbuffers(GlRenderPass renderPass, Collection<String> dynamicUniforms, CallbackInfoReturnable<Boolean> cir) {
		// Vanilla binds sampler objects to its texture units for each draw and leaves them bound.
		IrisRenderSystem.invalidateSamplerBindings();

		if (cir.getReturnValueZ()) {
			GbufferProgramBinder.afterSetup(renderPass);
		}
	}

	@ModifyExpressionValue(method = "drawFromBuffers", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/opengl/GlConst;toGl(Lcom/mojang/blaze3d/vertex/VertexFormat$Mode;)I"))
	private int iris$drawPatches(int mode) {
		return ImmediateState.usingTessellation && mode == GL40C.GL_TRIANGLES ? GL40C.GL_PATCHES : mode;
	}
}
