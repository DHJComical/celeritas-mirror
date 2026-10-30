package net.irisshaders.iris.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pathways.scaling.RenderScale;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.irisshaders.iris.uniforms.SystemTimeUniforms;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Util;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {
	@Inject(method = "render", at = @At("HEAD"))
	private void iris$startFrame(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo ci) {
		// This allows certain functions like float smoothing to function outside a world.
		CapturedRenderingState.INSTANCE.setRealTickDelta(deltaTracker.getGameTimeDeltaPartialTick(true));
		SystemTimeUniforms.COUNTER.beginFrame();
		SystemTimeUniforms.TIMER.beginFrame(Util.getNanos());
	}

	@Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setProjectionMatrix(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/ProjectionType;)V"))
	private void iris$captureProjection(CallbackInfo ci, @Local(ordinal = 0) Matrix4f projectionMatrix) {
		CapturedRenderingState.INSTANCE.setGbufferProjection(new Matrix4f(projectionMatrix));
	}

	// Everything renderLevel draws reads the main render target through the getter, so it all follows the scaled target.
	@Inject(method = "renderLevel", at = @At("HEAD"))
	private void iris$beginScaledWorld(CallbackInfo ci) {
		RenderScale.begin();
	}

	@Inject(method = "renderLevel", at = @At("TAIL"))
	private void iris$runColorSpace(CallbackInfo ci) {
		Iris.getPipelineManager().getPipeline().ifPresent(WorldRenderingPipeline::finalizeGameRendering);

		// After the color space pass, which works on the scaled image, and before the entity outline,
		// post chain and GUI, which draw at native resolution.
		RenderScale.end();
	}

	// Undoes the redirect if renderLevel exited before reaching its tail.
	@Inject(method = "render", at = @At("TAIL"))
	private void iris$ensureScaledWorldResolved(CallbackInfo ci) {
		RenderScale.end();
	}

	// The hand is drawn into the pack's gbuffers by HandRenderer instead.
	@WrapWithCondition(method = "renderItemInHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/player/LocalPlayer;I)V"))
	private boolean iris$skipVanillaHand(ItemInHandRenderer renderer, float partialTick, PoseStack poseStack, SubmitNodeCollector submits, LocalPlayer player, int lightCoords) {
		return !Iris.isPackInUseQuick();
	}
}
