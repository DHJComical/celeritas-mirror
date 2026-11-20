package net.irisshaders.iris.compat.sodium.mixin.clouds;

import com.mojang.blaze3d.systems.RenderSystem;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pipeline.ShaderRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.pipeline.programs.GbufferProgram;
import net.irisshaders.iris.pipeline.programs.ShaderKey;
import net.irisshaders.iris.pipeline.programs.VanillaDrawState;
import net.minecraft.client.Minecraft;
import org.embeddedt.embeddium.impl.gl.device.CommandList;
import org.embeddedt.embeddium.impl.render.clouds.CloudFrame;
import org.embeddedt.embeddium.impl.render.clouds.CloudRenderState;
import org.embeddedt.embeddium.impl.render.clouds.CloudRenderer;
import org.embeddedt.embeddium.impl.render.immediate.ModernCloudRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModernCloudRenderer.class)
public abstract class MixinCloudRenderer {
	@Shadow
	@Final
	private static CloudRenderState RENDER_STATE;

	@Unique
	private static final VanillaDrawState iris$drawState = new VanillaDrawState();

	/**
	 * @author embeddedt
	 * @reason draw the clouds with the shader pack's program, which generates the vertices from the same faces
	 */
	@Inject(method = "draw", at = @At("HEAD"), cancellable = true)
	private void iris$drawWithShaderPack(CommandList commandList, CloudRenderer renderer, CloudFrame frame, CallbackInfo ci) {
		WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();

		if (!(pipeline instanceof ShaderRenderingPipeline shaderPipeline)) {
			return;
		}

		GbufferProgram program = shaderPipeline.getShaderMap().getProgram(ShaderKey.CLOUDS_SODIUM);

		// Without a clouds program from the pack, our own draws them
		if (program == null) {
			return;
		}

		VanillaDrawState state = iris$drawState;
		state.modelView.set(frame.modelView());
		state.projection.set(frame.projection());
		state.textureMatrix.identity();
		state.chunkOffset.zero();
		// The cloud color is applied through the cloud info, so the pack mustn't apply it again
		state.colorModulator.set(1.0F);
		float[] fogColor = RenderSystem.getShaderFogColor();
		state.fogColor.set(fogColor[0], fogColor[1], fogColor[2], fogColor[3]);
		state.fogStart = RenderSystem.getShaderFogStart();
		state.fogEnd = RenderSystem.getShaderFogEnd();
		state.albedoTexture = RenderSystem.getShaderTexture(0);
		state.overlayTexture = RenderSystem.getShaderTexture(1);
		state.lightmapTexture = RenderSystem.getShaderTexture(2);
		state.cloudColor.set(frame.red(), frame.green(), frame.blue(), CloudRenderer.getAlpha());
		state.cloudOffset.set(renderer.getOffsetX(), renderer.getOffsetY(), renderer.getOffsetZ());
		state.cloudCellSize.set(renderer.getCellSizeX(), renderer.getCellHeight(), renderer.getCellSizeZ());
		state.cloudCenterCellX = renderer.getCenterCellX();
		state.cloudCenterCellZ = renderer.getCenterCellZ();

		RENDER_STATE.setup(frame.fancy());
		program.bind(state);
		renderer.bindTextures();
		renderer.draw(commandList);
		program.unbind();
		Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
		RENDER_STATE.restore();

		ci.cancel();
	}
}
