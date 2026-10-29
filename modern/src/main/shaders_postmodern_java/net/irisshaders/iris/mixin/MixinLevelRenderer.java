package net.irisshaders.iris.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.compat.dh.DHCompat;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.pipeline.WorldRenderingPhase;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.irisshaders.iris.uniforms.IrisTimeUniforms;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
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
			pipeline.setPhase(WorldRenderingPhase.NONE);
		});
	}

	@Inject(method = "renderLevel", at = @At("RETURN"))
	private void iris$endLevelRender(CallbackInfo ci) {
		Profiler.get().push("iris_final");
		iris$pipeline.finalizeLevelRendering();
		Profiler.get().pop();
		iris$pipeline = null;

		IrisRenderSystem.restoreCullingState();

		if (Iris.shouldActivateWireframe() && this.minecraft.isLocalServer()) {
			IrisRenderSystem.setPolygonMode(GL43C.GL_FILL);
		}
	}
}
