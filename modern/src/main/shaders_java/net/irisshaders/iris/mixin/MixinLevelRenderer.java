package net.irisshaders.iris.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.compat.dh.DHCompat;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.layer.IsOutlineRenderStateShard;
import net.irisshaders.iris.layer.OuterWrappedRenderType;
import net.irisshaders.iris.pathways.HandRenderer;
import net.irisshaders.iris.pipeline.WorldRenderingPhase;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.shadows.frustum.fallback.NonCullingFrustum;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.irisshaders.iris.uniforms.IrisTimeUniforms;
import net.irisshaders.iris.uniforms.SystemTimeUniforms;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.Frustum;
import org.embeddedt.embeddium.compat.mc.ICamera;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL43C;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {
	private static final String RENDER = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(Lcom/mojang/blaze3d/vertex/PoseStack;FJZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;)V";
	private static final String CLEAR = "Lcom/mojang/blaze3d/systems/RenderSystem;clear(IZ)V";
	private static final String RENDER_SKY = "Lnet/minecraft/client/renderer/LevelRenderer;renderSky(Lcom/mojang/blaze3d/vertex/PoseStack;Lorg/joml/Matrix4f;FLnet/minecraft/client/Camera;ZLjava/lang/Runnable;)V";
	private static final String RENDER_CLOUDS = "Lnet/minecraft/client/renderer/LevelRenderer;renderClouds(Lcom/mojang/blaze3d/vertex/PoseStack;Lorg/joml/Matrix4f;FDDD)V";
	private static final String RENDER_WEATHER = "Lnet/minecraft/client/renderer/LevelRenderer;renderSnowAndRain(Lnet/minecraft/client/renderer/LightTexture;FDDD)V";


    private static WorldRenderingPhase fromTerrainRenderType(RenderType renderType) {
        if (renderType == RenderType.solid()) {
            return WorldRenderingPhase.TERRAIN_SOLID;
        } else if (renderType == RenderType.cutout()) {
            return WorldRenderingPhase.TERRAIN_CUTOUT;
        } else if (renderType == RenderType.cutoutMipped()) {
            return WorldRenderingPhase.TERRAIN_CUTOUT_MIPPED;
        } else if (renderType == RenderType.translucent()) {
            return WorldRenderingPhase.TERRAIN_TRANSLUCENT;
        } else if (renderType == RenderType.tripwire()) {
            return WorldRenderingPhase.TRIPWIRE;
        } else {
            throw new IllegalStateException("Illegal render type!");
        }
    }

	@Shadow
	@Final
	private Minecraft minecraft;

	@Unique
	private WorldRenderingPipeline pipeline;

	@Shadow
	private RenderBuffers renderBuffers;

	@Shadow
	private int ticks;

	@Shadow
	private Frustum cullingFrustum;

	@Shadow
	private @Nullable ClientLevel level;

    @Unique
    private float celeritas$currentTickDelta;

	// Begin shader rendering after buffers have been cleared.
	// At this point we've ensured that Minecraft's main framebuffer is cleared.
	// This is important or else very odd issues will happen with shaders that have a final pass that doesn't write to
	// all pixels.
	@Inject(method = "renderLevel", at = @At("HEAD"))
	private void iris$setupPipeline(CallbackInfo callback,
                                    //? if <1.21 {
                                    @Local(ordinal = 0, argsOnly = true) float tickDelta,
                                    //?} else
                                    /*@Local(ordinal = 0, argsOnly = true) net.minecraft.client.DeltaTracker deltaTracker,*/
                                    //? if <1.20.6 {
                                    @Local(ordinal = 0, argsOnly = true) PoseStack poseStack,
                                    @Local(ordinal = 0, argsOnly = true) Matrix4f projection
                                    //?} else {
                                    /*@Local(ordinal = 0, argsOnly = true) Matrix4f modelView,
                                    @Local(ordinal = 1, argsOnly = true) Matrix4f projection
                                    *///?}
    ) {
		DHCompat.checkFrame();

        //? if >=1.21
        /*float tickDelta = deltaTracker.getGameTimeDeltaPartialTick(true);*/

        this.celeritas$currentTickDelta = tickDelta;

		IrisTimeUniforms.updateTime();
        //? if <1.20.6
        Matrix4f modelView = poseStack.last().pose();
		CapturedRenderingState.INSTANCE.setGbufferModelView(modelView);
		CapturedRenderingState.INSTANCE.setGbufferProjection(projection);
        //? if >=1.20.4 {
        /*net.minecraft.world.TickRateManager lvTickRateManager10 = this.minecraft.level.tickRateManager();
        float fakeTickDelta = lvTickRateManager10.runsNormally() ? tickDelta : 1.0F;
        *///?} else
        float fakeTickDelta = tickDelta;
		CapturedRenderingState.INSTANCE.setTickDelta(fakeTickDelta);
		CapturedRenderingState.INSTANCE.setCloudTime((ticks + fakeTickDelta) * 0.03F);

		pipeline = Iris.getPipelineManager().preparePipeline(Iris.getCurrentDimension());

		if (pipeline.shouldDisableFrustumCulling()) {
			this.cullingFrustum = new NonCullingFrustum();
		}

        IrisRenderSystem.backupAndDisableCullingState(pipeline.shouldDisableOcclusionCulling());

		if (Iris.shouldActivateWireframe() && this.minecraft.isLocalServer()) {
			IrisRenderSystem.setPolygonMode(GL43C.GL_LINE);
		}
	}

	// Begin shader rendering after buffers have been cleared.
	// At this point we've ensured that Minecraft's main framebuffer is cleared.
	// This is important or else very odd issues will happen with shaders that have a final pass that doesn't write to
	// all pixels.
	@Inject(method = "renderLevel", at = @At(value = "INVOKE", target = CLEAR, shift = At.Shift.AFTER))
	private void iris$beginLevelRender(CallbackInfo callback) {
		pipeline.beginLevelRendering();
		pipeline.setPhase(WorldRenderingPhase.NONE);
	}


	// Inject a bit early so that we can end our rendering before mods like VoxelMap (which inject at RETURN)
	// render their waypoint beams.
	@Inject(method = "renderLevel", at = @At(value = "RETURN", shift = At.Shift.BEFORE))
	private void iris$endLevelRender(CallbackInfo callback,
                                     //? if <1.20.6 {
                                     @Local(ordinal = 0, argsOnly = true) PoseStack poseStack,
                                     //?} else
                                     /*@Local(ordinal = 0, argsOnly = true) Matrix4f modelMatrix,*/
                                     @Local(ordinal = 0, argsOnly = true) Camera camera,
                                     @Local(ordinal = 0, argsOnly = true) GameRenderer gameRenderer
                                     ) {
        //? if >=1.20.6
        /*var poseStack = new PoseStack();*/
		HandRenderer.INSTANCE.render(HandRenderer.Stage.TRANSLUCENT, poseStack, celeritas$currentTickDelta, camera, gameRenderer, pipeline);
		Minecraft.getInstance().getProfiler().popPush("iris_final");
		pipeline.finalizeLevelRendering();
		pipeline = null;

        IrisRenderSystem.restoreCullingState();

		if (Iris.shouldActivateWireframe() && this.minecraft.isLocalServer()) {
			IrisRenderSystem.setPolygonMode(GL43C.GL_FILL);
		}
	}

	// Setup shadow terrain & render shadows before the main terrain setup. We need to do things in this order to
	// avoid breaking other mods such as Light Overlay: https://github.com/IrisShaders/Iris/issues/1356

	// Do this before sky rendering so it's ready before the sky render starts.
	@Inject(method = "renderLevel", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", args = "ldc=sky"))
	private void iris$renderTerrainShadows(CallbackInfo ci, @Local(ordinal = 0, argsOnly = true) Camera camera) {
		pipeline.renderShadows((LevelRendererAccessor) this, (ICamera) camera);
	}

	@ModifyVariable(method = "renderSky", at = @At(value = "HEAD"), index = 5, argsOnly = true)
	private boolean iris$alwaysRenderSky(boolean value) {
		return false;
	}

	@Inject(method = "renderLevel", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", args = "ldc=sky"))
	private void iris$beginSky(CallbackInfo callback) {
		// Use CUSTOM_SKY until levelFogColor is called as a heuristic to catch FabricSkyboxes.
		pipeline.setPhase(WorldRenderingPhase.CUSTOM_SKY);

		// We've changed the phase, but vanilla doesn't update the shader program at this point before rendering stuff,
		// so we need to manually refresh the shader program so that the correct shader override gets applied.
		// TODO: Move the injection instead
		RenderSystem.setShader(GameRenderer::getPositionShader);
	}

	@Inject(method = "renderSky",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/FogRenderer;levelFogColor()V"))
	private void iris$renderSky$beginNormalSky(CallbackInfo ci) {
		// None of the vanilla sky is rendered until after this call, so if anything is rendered before, it's
		// CUSTOM_SKY.
		pipeline.setPhase(WorldRenderingPhase.SKY);
	}

	@Inject(method = "renderSky", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;SUN_LOCATION:Lnet/minecraft/resources/ResourceLocation;"))
	private void iris$setSunRenderStage(CallbackInfo ci) {
		pipeline.setPhase(WorldRenderingPhase.SUN);
	}

	@Inject(method = "renderSky", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/DimensionSpecialEffects;getSunriseColor(FF)[F"))
	private void iris$setSunsetRenderStage(CallbackInfo ci) {
		pipeline.setPhase(WorldRenderingPhase.SUNSET);
	}

	@Inject(method = "renderSky", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;MOON_LOCATION:Lnet/minecraft/resources/ResourceLocation;"))
	private void iris$setMoonRenderStage(CallbackInfo ci) {
		pipeline.setPhase(WorldRenderingPhase.MOON);
	}

	@Inject(method = "renderSky", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getStarBrightness(F)F"))
	private void iris$setStarRenderStage(CallbackInfo ci) {
		pipeline.setPhase(WorldRenderingPhase.STARS);
	}

	@Inject(method = "renderSky", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getEyePosition(F)Lnet/minecraft/world/phys/Vec3;"))
	private void iris$setVoidRenderStage(CallbackInfo ci) {
		pipeline.setPhase(WorldRenderingPhase.VOID);
	}

	@Inject(method = "renderSky", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getTimeOfDay(F)F"),
		slice = @Slice(from = @At(value = "FIELD", target = "Lcom/mojang/math/Axis;YP:Lcom/mojang/math/Axis;")))
	private void iris$renderSky$tiltSun(CallbackInfo ci, @Local(ordinal = 0) PoseStack poseStack) {
		poseStack.mulPose(Axis.ZP.rotationDegrees(pipeline.getSunPathRotation()));
	}

	@Inject(method = "renderLevel", at = @At(value = "INVOKE", target =
            //? if <1.21 {
            "Lnet/minecraft/client/renderer/LevelRenderer;renderSky(Lcom/mojang/blaze3d/vertex/PoseStack;Lorg/joml/Matrix4f;FLnet/minecraft/client/Camera;ZLjava/lang/Runnable;)V"
            //?} else
            /*"Lnet/minecraft/client/renderer/LevelRenderer;renderSky(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FLnet/minecraft/client/Camera;ZLjava/lang/Runnable;)V"*/
            , shift = At.Shift.AFTER))
	private void iris$endSky(CallbackInfo ci) {
		pipeline.setPhase(WorldRenderingPhase.NONE);
	}

	@Inject(method = "renderClouds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/DimensionSpecialEffects;getCloudHeight()F", shift = At.Shift.AFTER))
	private void iris$beginClouds(CallbackInfo ci) {
		pipeline.setPhase(WorldRenderingPhase.CLOUDS);
	}

	@Inject(method = "renderClouds", at = @At("RETURN"))
	private void iris$endClouds(CallbackInfo ci) {
		pipeline.setPhase(WorldRenderingPhase.NONE);
	}

    private static final String RENDER_CHUNK_LAYER = /*? if <1.20.2 {*/ "renderChunkLayer" /*?} else {*/ /*"renderSectionLayer" *//*?}*/;

	@Inject(method = RENDER_CHUNK_LAYER, at = @At("HEAD"))
	private void iris$beginTerrainLayer(CallbackInfo ci, @Local(ordinal = 0, argsOnly = true) RenderType renderType) {
		pipeline.setPhase(fromTerrainRenderType(renderType));
	}

	@Inject(method = RENDER_CHUNK_LAYER, at = @At("RETURN"))
	private void iris$endTerrainLayer(CallbackInfo ci) {
		pipeline.setPhase(WorldRenderingPhase.NONE);
	}

	@Inject(method = RENDER_WEATHER, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getRainLevel(F)F", shift = At.Shift.AFTER))
	private void iris$beginWeather(LightTexture arg, float tickDelta, double x, double y, double z, CallbackInfo callback) {
		pipeline.setPhase(WorldRenderingPhase.RAIN_SNOW);
	}

	@ModifyArg(method = RENDER_WEATHER, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;depthMask(Z)V", ordinal = 0))
	private boolean iris$writeRainAndSnowToDepthBuffer(boolean depthMaskEnabled) {
		if (pipeline.shouldWriteRainAndSnowToDepthBuffer()) {
			return true;
		}

		return depthMaskEnabled;
	}

	@Inject(method = RENDER_WEATHER, at = @At("RETURN"))
	private void iris$endWeather(LightTexture arg, float tickDelta, double x, double y, double z, CallbackInfo callback) {
		pipeline.setPhase(WorldRenderingPhase.NONE);
	}

	@Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderWorldBorder(Lnet/minecraft/client/Camera;)V"))
	private void iris$beginWorldBorder( CallbackInfo callback) {
		pipeline.setPhase(WorldRenderingPhase.WORLD_BORDER);
	}

	@Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderWorldBorder(Lnet/minecraft/client/Camera;)V", shift = At.Shift.AFTER))
	private void iris$endWorldBorder(CallbackInfo callback) {
		pipeline.setPhase(WorldRenderingPhase.NONE);
	}

	@Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/debug/DebugRenderer;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;DDD)V"))
	private void iris$setDebugRenderStage(CallbackInfo ci) {
		pipeline.setPhase(WorldRenderingPhase.DEBUG);
	}

	@Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/debug/DebugRenderer;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;DDD)V", shift = At.Shift.AFTER))
	private void iris$resetDebugRenderStage(CallbackInfo ci) {
		pipeline.setPhase(WorldRenderingPhase.NONE);
	}

	@ModifyArg(method = "renderLevel",
		at = @At(value = "INVOKE", target = "net/minecraft/client/renderer/MultiBufferSource$BufferSource.getBuffer (Lnet/minecraft/client/renderer/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"),
		slice = @Slice(
			from = @At(value = "CONSTANT", args = "stringValue=outline"),
			to = @At(value = "INVOKE", target = "net/minecraft/client/renderer/LevelRenderer.renderHitOutline (Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V")
		))
	private RenderType iris$beginBlockOutline(RenderType type) {
		return new OuterWrappedRenderType("iris:is_outline", type, IsOutlineRenderStateShard.INSTANCE);
	}

	@Inject(method = "renderLevel", at = @At(value = "CONSTANT", args = "stringValue=translucent"))
	private void iris$beginTranslucents(CallbackInfo callback,
                                        //? if <1.20.6 {
                                        @Local(ordinal = 0, argsOnly = true) PoseStack poseStack,
                                         //?} else
                                        /*@Local(ordinal = 0, argsOnly = true) Matrix4f modelMatrix,*/
                                        @Local(ordinal = 0, argsOnly = true) Camera camera,
                                        @Local(ordinal = 0, argsOnly = true) GameRenderer gameRenderer) {
        //? if >=1.20.6
        /*var poseStack = new PoseStack();*/
		pipeline.beginHand();
		HandRenderer.INSTANCE.render(HandRenderer.Stage.SOLID, poseStack, celeritas$currentTickDelta, camera, gameRenderer, pipeline);
		Minecraft.getInstance().getProfiler().popPush("iris_pre_translucent");
		pipeline.beginTranslucents();
	}
}
