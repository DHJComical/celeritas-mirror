package net.irisshaders.iris.mixin.fantastic;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Matrix4f;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.fantastic.ParticleRenderingPhase;
import net.irisshaders.iris.fantastic.PhasedParticleEngine;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.shaderpack.properties.ParticleRenderingSettings;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Uses the PhasedParticleManager changes to render opaque particles much earlier than other particles.
 * <p>
 * See the comments in {@link MixinParticleEngine} for more details.
 */
@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Shadow
	private RenderBuffers renderBuffers;

	@Inject(method = "renderLevel", at = @At("HEAD"))
	private void iris$resetParticleManagerPhase(CallbackInfo ci) {
		((PhasedParticleEngine) minecraft.particleEngine).setParticleRenderingPhase(ParticleRenderingPhase.EVERYTHING);
	}

	@Inject(method = "renderLevel", at = @At(value = "CONSTANT", args = "stringValue=entities"))
	private void iris$renderOpaqueParticles(CallbackInfo ci, @Local(ordinal = 0) Frustum vanillaFrustum,
                                            //? if <1.20.6
                                            @Local(ordinal = 0, argsOnly = true) PoseStack poseStack,
                                            @Local(ordinal = 0, argsOnly = true) LightTexture lightTexture,
                                            @Local(ordinal = 0, argsOnly = true) Camera camera,
                                            @Local(ordinal = 0, argsOnly = true) float f
                                            ) {
		minecraft.getProfiler().popPush("opaque_particles");

		MultiBufferSource.BufferSource bufferSource = renderBuffers.bufferSource();

		ParticleRenderingSettings settings = getRenderingSettings();

        boolean isRendering = false;

		if (settings == ParticleRenderingSettings.BEFORE) {
            isRendering = true;
		} else if (settings == ParticleRenderingSettings.MIXED) {
			((PhasedParticleEngine) minecraft.particleEngine).setParticleRenderingPhase(ParticleRenderingPhase.OPAQUE);
            isRendering = true;
		}

        if (isRendering) {
            minecraft.particleEngine.render(/*? if <1.20.6 {*/ poseStack, bufferSource, /*?}*/ lightTexture, camera, f /*? if forgelike {*/, vanillaFrustum /*?}*/);

            // Workaround: Restore some render state that modded particles tend to break.
            celeritas$restoreNormalRenderState();
        }
	}

    private void celeritas$restoreNormalRenderState() {
        RenderSystem.enableCull();
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", args = "ldc=particles"))
    private void iris$setRenderingPhaseForVanillaParticles(CallbackInfo ci) {
        ParticleRenderingSettings settings = getRenderingSettings();

        ParticleRenderingPhase phase;
        if (settings == ParticleRenderingSettings.AFTER) {
            phase = ParticleRenderingPhase.EVERYTHING;
        } else if (settings == ParticleRenderingSettings.MIXED) {
            phase = ParticleRenderingPhase.TRANSLUCENT;
        } else {
            phase = ParticleRenderingPhase.NOTHING;
        }

        ((PhasedParticleEngine) minecraft.particleEngine).setParticleRenderingPhase(phase);
    }

	private ParticleRenderingSettings getRenderingSettings() {
		return Iris.getPipelineManager().getPipeline().map(WorldRenderingPipeline::getParticleRenderingSettings).orElse(ParticleRenderingSettings.MIXED);
	}
}
