package net.irisshaders.iris.mixin.fantastic;

import javax.annotation.Nullable;

import static com.mitchej123.glsm.RenderSystemService.RENDER_SYSTEM;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Matrix4f;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.fantastic.ParticleRenderingPhase;
import net.irisshaders.iris.fantastic.PhasedParticleEngine;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.shaderpack.properties.ParticleRenderingSettings;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;

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

    @Shadow
    @Nullable
    private PostChain transparencyChain;

    @Inject(method = "renderLevel", at = @At("HEAD"))
	private void iris$resetParticleManagerPhase(CallbackInfo ci) {
		((PhasedParticleEngine) minecraft.particleEngine).setParticleRenderingPhase(ParticleRenderingPhase.EVERYTHING);
	}

	@Inject(method = "renderLevel", at = @At(value = "CONSTANT", args = "stringValue=translucent"))
	private void iris$renderOpaqueParticles(CallbackInfo ci, @Local(ordinal = 0) Frustum vanillaFrustum,
                                            //? if <1.20.6
                                            @Local(ordinal = 0, argsOnly = true) PoseStack poseStack,
                                            @Local(ordinal = 0, argsOnly = true) LightTexture lightTexture,
                                            @Local(ordinal = 0, argsOnly = true) Camera camera,
                                            //? if <1.21 {
                                            @Local(ordinal = 0, argsOnly = true) float f
                                            //?} else
                                            /*@Local(ordinal = 0, argsOnly = true) net.minecraft.client.DeltaTracker deltaTracker*/
                                            ) {

        if (this.transparencyChain != null) {
            return;
        }

        //? if >=1.21
        /*float f = deltaTracker.getGameTimeDeltaPartialTick(false);*/
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
            minecraft.particleEngine.render(/*? if <1.20.6 {*/ poseStack, bufferSource, /*?}*/ lightTexture, camera, f /*? if forgelike {*/, vanillaFrustum /*?}*/ /*? if neoforge && >=1.21 {*/ /*, t -> true *//*?}*/);

            // Workaround: Restore some render state that modded particles tend to break.
            celeritas$restoreNormalRenderState();
        }
	}

    private void celeritas$restoreNormalRenderState() {
        RENDER_SYSTEM.enableCullFace();
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

    //? if neoforge && >=1.21 {
    /*/^*
     * @author embeddedt
     * @reason disable Neo's particle phasing, because we do it ourselves
     ^/
    @ModifyArg(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V"))
    private Predicate<ParticleRenderType> forceAllTypes(Predicate<ParticleRenderType> oldPredicate) {
        return t -> true;
    }

    @Redirect(method = "renderLevel", slice = @Slice(from = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", args = "ldc=solid_particles")), at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V", ordinal = 0))
    private void disableNeoSolidParticleRendering(ParticleEngine instance, LightTexture crashreportcategory, Camera throwable, float particle, Frustum meshdata, Predicate<ParticleRenderType> tesselator) {

    }
    *///?}

	private ParticleRenderingSettings getRenderingSettings() {
		return Iris.getPipelineManager().getPipeline().map(WorldRenderingPipeline::getParticleRenderingSettings).orElse(ParticleRenderingSettings.MIXED);
	}
}
