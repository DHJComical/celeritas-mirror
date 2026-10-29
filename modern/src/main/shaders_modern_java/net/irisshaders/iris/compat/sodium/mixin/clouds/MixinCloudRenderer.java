package net.irisshaders.iris.compat.sodium.mixin.clouds;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.VertexBuffer;
import org.embeddedt.embeddium.impl.render.immediate.CloudRenderer;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pipeline.ShaderRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.pipeline.programs.ShaderKey;
import org.embeddedt.embeddium.impl.render.immediate.CloudShader;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CloudRenderer.class)
public abstract class MixinCloudRenderer {
	@Shadow private VertexBuffer vertexBuffer;

	/**
	 * @author embeddedt
	 * @reason draw the clouds using the Iris shader if present
	 */
	@WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lorg/embeddedt/embeddium/impl/render/immediate/CloudShader;prepareForDraw(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FFFF)V"))
	private void drawWithIrisShader(CloudShader instance, Matrix4f modelViewMatrix, Matrix4f projectionMatrix, float r, float g, float b, float a, Operation<Void> original) {
		WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();

		if (pipeline instanceof ShaderRenderingPipeline) {
			var irisShader = ((ShaderRenderingPipeline) pipeline).getShaderMap().getShader(ShaderKey.CLOUDS_SODIUM);

			this.vertexBuffer.drawWithShader(modelViewMatrix, projectionMatrix, irisShader);
		} else {
			original.call(instance, modelViewMatrix, projectionMatrix, r, g, b, a);
		}
	}

	/**
	 * @author embeddedt
	 * @reason suppress the vanilla cloud rendering if Iris shader is active
	 */
	@Inject(method = "drawVertexBuffer", at = @At("HEAD"), cancellable = true)
	private void drawWithIrisShader(CallbackInfo ci) {
		WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();

		if (pipeline instanceof ShaderRenderingPipeline) {
			ci.cancel();
		}
	}
}
