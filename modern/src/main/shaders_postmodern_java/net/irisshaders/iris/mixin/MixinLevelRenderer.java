package net.irisshaders.iris.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.textures.GpuTexture;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.compat.dh.DHCompat;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.layer.GbufferPrograms;
import net.irisshaders.iris.pathways.HandRenderer;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPhase;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.shadows.frustum.fallback.NonCullingFrustum;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.irisshaders.iris.uniforms.IrisTimeUniforms;
import net.irisshaders.iris.vertices.ImmediateState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.profiling.Profiler;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL43C;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Shadow
	@Final
	private LevelTargetBundle targets;

	@Shadow
	private int ticks;

	@Shadow
	private ClientLevel level;

	@Unique
	private WorldRenderingPipeline iris$pipeline;

	@Inject(method = "renderLevel", at = @At("HEAD"))
	private void iris$setupPipeline(GraphicsResourceAllocator resourceAllocator, DeltaTracker deltaTracker, boolean renderOutline,
									CameraRenderState cameraState, Matrix4fc modelViewMatrix, GpuBufferSlice terrainFog, Vector4f fogColor,
									boolean shouldRenderSky, ChunkSectionsToRender chunkSectionsToRender, CallbackInfo ci) {
		DHCompat.checkFrame();

		IrisTimeUniforms.updateTime();
		CapturedRenderingState.INSTANCE.setGbufferModelView(new Matrix4f(modelViewMatrix));

		float tickDelta = deltaTracker.getGameTimeDeltaPartialTick(true);
		float fakeTickDelta = level.tickRateManager().runsNormally() ? tickDelta : 1.0F;
		CapturedRenderingState.INSTANCE.setTickDelta(fakeTickDelta);
		CapturedRenderingState.INSTANCE.setCloudTime((ticks + fakeTickDelta) * 0.03F);

		iris$pipeline = Iris.getPipelineManager().preparePipeline(Iris.getCurrentDimension());
		ImmediateState.isRenderingLevel = true;

		IrisRenderSystem.backupAndDisableCullingState(iris$pipeline.shouldDisableOcclusionCulling());

		if (Iris.shouldActivateWireframe() && this.minecraft.isLocalServer()) {
			IrisRenderSystem.setPolygonMode(GL43C.GL_LINE);
		}
	}

	/**
	 * Begins shader rendering in a pass of its own right after the main target is cleared, since packs whose final
	 * pass doesn't write every pixel rely on it being cleared first.
	 */
	@Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/framegraph/FramePass;executes(Ljava/lang/Runnable;)V", ordinal = 0, shift = At.Shift.AFTER))
	private void iris$addBeginPass(CallbackInfo ci, @Local FrameGraphBuilder frame) {
		WorldRenderingPipeline pipeline = iris$pipeline;
		FramePass pass = frame.addPass("iris_begin");
		targets.main = pass.readsAndWrites(targets.main);
		pass.executes(() -> {
			pipeline.beginLevelRendering();
			pipeline.renderShadows((LevelRenderer) (Object) this, Minecraft.getInstance().gameRenderer.getMainCamera());
			// Whatever is drawn before vanilla's sky disc, such as by skybox mods, is a custom sky.
			pipeline.setPhase(WorldRenderingPhase.CUSTOM_SKY);
		});
	}

	@Inject(method = "renderLevel", at = @At("RETURN"))
	private void iris$endLevelRender(CallbackInfo ci) {
		HandRenderer.INSTANCE.renderTranslucent();

		Profiler.get().push("iris_final");
		iris$pipeline.finalizeLevelRendering();
		Profiler.get().pop();
		ImmediateState.isRenderingLevel = false;
		iris$pipeline = null;

		IrisRenderSystem.restoreCullingState();

		if (Iris.shouldActivateWireframe() && this.minecraft.isLocalServer()) {
			IrisRenderSystem.setPolygonMode(GL43C.GL_FILL);
		}
	}

	// The outline is only drawn once the buffer source's batches end, so its phase is ended by whatever comes next.
	@Inject(method = "renderBlockOutline", at = @At("HEAD"))
	private void iris$beginOutline(CallbackInfo ci) {
		GbufferPrograms.setPhase(WorldRenderingPhase.OUTLINE);
	}

	@ModifyExpressionValue(method = {"update", "extractLevel"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getCullFrustum()Lnet/minecraft/client/renderer/culling/Frustum;"))
	private Frustum iris$disableFrustumCulling(Frustum frustum) {
		WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();

		return pipeline != null && pipeline.shouldDisableFrustumCulling() ? new NonCullingFrustum() : frustum;
	}

	// Vanilla clears depth to draw debug overlays on top, but shader packs still read it in their composite passes.
	@WrapWithCondition(method = "lambda$addLateDebugPass$0", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/CommandEncoder;clearDepthTexture(Lcom/mojang/blaze3d/textures/GpuTexture;D)V"))
	private boolean iris$keepDepthForComposites(CommandEncoder encoder, GpuTexture texture, double depth) {
		return !(Iris.getPipelineManager().getPipelineNullable() instanceof IrisRenderingPipeline);
	}
}
