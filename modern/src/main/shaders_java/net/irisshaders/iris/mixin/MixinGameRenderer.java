package net.irisshaders.iris.mixin;

import com.google.common.collect.Lists;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.GlUtil;
import com.mojang.blaze3d.shaders.Program;
import com.mojang.blaze3d.vertex.PoseStack;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.gl.program.IrisProgramTypes;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.irisshaders.iris.uniforms.SystemTimeUniforms;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.taumc.celeritas.shaders.CeleritasShaders;

import java.util.ArrayList;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {
	@Shadow
	private boolean renderHand;

    @Inject(method = "render", at = @At("HEAD"))
    private void iris$startFrame(CallbackInfo ci,
                                 //? if <1.21 {
                                 @Local(ordinal = 0, argsOnly = true) float tickDelta
                                 //?} else
                                 /*@Local(ordinal = 0, argsOnly = true) net.minecraft.client.DeltaTracker deltaTracker*/
                                 ) {
        // This allows certain functions like float smoothing to function outside a world.
        CapturedRenderingState.INSTANCE.setRealTickDelta(
                //? if <1.21 {
                tickDelta
                //?} else
                /*deltaTracker.getGameTimeDeltaPartialTick(true)*/
        );
        SystemTimeUniforms.COUNTER.beginFrame();
        SystemTimeUniforms.TIMER.beginFrame(Util.getNanos());
    }

	@Inject(method = "<init>", at = @At("TAIL"))
	private void iris$logSystem(Minecraft arg, ItemInHandRenderer arg2, ResourceManager arg3, RenderBuffers arg4, CallbackInfo ci) {
		CeleritasShaders.logger().info("Hardware information:");
		CeleritasShaders.logger().info("CPU: " + GlUtil.getCpuInfo());
		CeleritasShaders.logger().info("GPU: " + GlUtil.getRenderer() + " (Supports OpenGL " + GlUtil.getOpenGLVersion() + ")");
		CeleritasShaders.logger().info("OS: " + System.getProperty("os.name") + " (" + System.getProperty("os.version") + ")");
	}

	@WrapWithCondition(method = "renderItemInHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/player/LocalPlayer;I)V"))
	private boolean iris$disableVanillaHandRendering(ItemInHandRenderer itemInHandRenderer, float tickDelta, PoseStack poseStack, BufferSource bufferSource, LocalPlayer localPlayer, int light) {
        return !IrisApi.getInstance().isShaderPackInUse();
    }

	@Inject(method = "renderLevel", at = @At("TAIL"))
	private void iris$runColorSpace(CallbackInfo ci) {
		Iris.getPipelineManager().getPipeline().ifPresent(WorldRenderingPipeline::finalizeGameRendering);
	}

	@Redirect(method = "reloadShaders", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Lists;newArrayList()Ljava/util/ArrayList;"))
	private ArrayList<Program> iris$reloadGeometryShaders() {
		ArrayList<Program> programs = Lists.newArrayList();
		programs.addAll(IrisProgramTypes.GEOMETRY.getPrograms().values());
		programs.addAll(IrisProgramTypes.TESS_CONTROL.getPrograms().values());
		programs.addAll(IrisProgramTypes.TESS_EVAL.getPrograms().values());
		return programs;
	}
}
