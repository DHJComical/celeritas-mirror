package net.irisshaders.iris.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pipeline.WorldRenderingPhase;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ensures that all particles are rendered with the textured_lit shader program.
 */
@Mixin(value = ParticleEngine.class, remap = false)
public class MixinParticleEngine {
	private static final String RENDER =
		"Lnet/minecraft/client/particle/ParticleEngine;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;)V";

	@Inject(method = RENDER, at = @At("HEAD"))
	private void iris$beginDrawingParticles(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,
											LightTexture lightTexture, Camera camera, float f, Frustum frustum,
											CallbackInfo ci) {
		Iris.getPipelineManager().getPipeline().ifPresent(pipeline -> pipeline.setPhase(WorldRenderingPhase.PARTICLES));
	}

	@Inject(method = RENDER, at = @At("RETURN"))
	private void iris$finishDrawingParticles(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,
											 LightTexture lightTexture, Camera camera, float f, Frustum frustum,
											 CallbackInfo ci) {
		Iris.getPipelineManager().getPipeline().ifPresent(pipeline -> pipeline.setPhase(WorldRenderingPhase.NONE));
	}
}
