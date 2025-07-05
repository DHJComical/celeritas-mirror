package org.taumc.celeritas.mixin.shaders;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.irisshaders.iris.IrisArchaic;
import net.irisshaders.iris.IrisCommon;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.pathways.HandRenderer;
import net.irisshaders.iris.pipeline.WorldRenderingPhase;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.shaderpack.DimensionId;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.irisshaders.iris.uniforms.IrisTimeUniforms;
import net.irisshaders.iris.uniforms.SystemTimeUniforms;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.EntityLivingBase;
import org.embeddedt.embeddium.compat.mc.MCLevelRenderer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL43C;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.taumc.celeritas.api.v0.CeleritasShadersApi;
import org.taumc.celeritas.compat.Camera;

@Mixin(EntityRenderer.class)
public class MixinEntityRenderer_Shaders implements MCLevelRenderer {
    @Shadow public Minecraft mc;

    @Unique
    private WorldRenderingPipeline pipeline;

    @Unique
    private double celeritas$currentTickDelta;

    @Inject(method="renderWorld(FJ)V", at=@At(value="HEAD"))
    private void iris$setupPipeline(float partialTicks, long limitTime, CallbackInfo ci) {
        // DHCompat.checkFrame();

        this.celeritas$currentTickDelta = partialTicks;

        IrisTimeUniforms.updateTime();

        CapturedRenderingState.INSTANCE.setTickDelta((float) partialTicks);
        // TODO: Figure out world time/ticks
        CapturedRenderingState.INSTANCE.setCloudTime((float)(this.mc.renderViewEntity.worldObj.getWorldTime() + partialTicks) * 0.03F);

        // pipeline = IrisCommon.getPipelineManager().preparePipeline(IrisArchaic.getCurrentDimension());
        // TODO: getCurrentDimension
        pipeline =  IrisCommon.getPipelineManager().preparePipeline(DimensionId.OVERWORLD);

        // if (pipeline.shouldDisableFrustumCulling()) {
        //     this.cullingFrustum = new NonCullingFrustum();
        // }

        IrisRenderSystem.backupAndDisableCullingState(pipeline.shouldDisableOcclusionCulling());

        if (IrisArchaic.shouldActivateWireframe() && mc.isSingleplayer()) {
             IrisRenderSystem.setPolygonMode(GL43C.GL_LINE);
         }

        pipeline.setPhase(WorldRenderingPhase.NONE);
    }
    @Inject(method="renderWorld(FJ)V", at=@At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;sortAndRender(Lnet/minecraft/entity/EntityLivingBase;ID)I", ordinal = 0))
    private void iris$beginTerrain(float partialTicks, long limitTime, CallbackInfo ci) {
        pipeline.setPhase(WorldRenderingPhase.TERRAIN_CUTOUT);
    }


    @Inject(method="renderWorld(FJ)V",
        at=@At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;sortAndRender(Lnet/minecraft/entity/EntityLivingBase;ID)I"),
        slice=@Slice(from=@At(value="INVOKE", target ="Lnet/minecraft/client/renderer/RenderGlobal;drawBlockDamageTexture(Lnet/minecraft/client/renderer/Tessellator;Lnet/minecraft/entity/EntityLivingBase;F)V"))
    )
    private void iris$beginTranslucentRender(float partialTicks, long limitTime, CallbackInfo ci) {
        final Camera camera = new Camera(mc.renderViewEntity, partialTicks);
        pipeline.beginHand();
        HandRenderer.INSTANCE.renderSolid(camera.getPartialTicks(), camera, mc.renderGlobal, pipeline);
        mc.mcProfiler.endStartSection("iris_pre_translucent");

        pipeline.setPhase(WorldRenderingPhase.TERRAIN_TRANSLUCENT);
        pipeline.beginTranslucents();
    }


    @Inject(method = "renderWorld(FJ)V", at = @At(value="INVOKE", target="Lorg/lwjgl/opengl/GL11;glClear(I)V", shift=At.Shift.AFTER, ordinal=0))
    private void iris$beginLevelRender(CallbackInfo callback) {
        pipeline.beginLevelRendering();
        pipeline.setPhase(WorldRenderingPhase.NONE);
    }

    @Inject(method = "renderWorld(FJ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/culling/ClippingHelperImpl;getInstance()Lnet/minecraft/client/renderer/culling/ClippingHelper;", shift = At.Shift.AFTER, ordinal = 0))
    private void iris$startFrame(float partialTicks, long startTime, CallbackInfo ci) {
        CapturedRenderingState.INSTANCE.setTickDelta(partialTicks);
        SystemTimeUniforms.COUNTER.beginFrame();
        SystemTimeUniforms.TIMER.beginFrame(System.nanoTime());
    }


    @Inject(method = "renderWorld(FJ)V", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/ForgeHooksClient;dispatchRenderLast(Lnet/minecraft/client/renderer/RenderGlobal;F)V", remap = false))
    private void iris$endLevelRender(float partialTicks, long limitTime, CallbackInfo callback) {
        final Camera camera = new Camera(mc.renderViewEntity, partialTicks);
        HandRenderer.INSTANCE.renderTranslucent(partialTicks, camera, mc.renderGlobal, pipeline);
        Minecraft.getMinecraft().mcProfiler.endStartSection("iris_final");
        pipeline.finalizeLevelRendering();
        pipeline = null;

        IrisRenderSystem.restoreCullingState();

         if (IrisArchaic.shouldActivateWireframe() && mc.isSingleplayer()) {
             IrisRenderSystem.setPolygonMode(GL43C.GL_FILL);
         }
    }


    @WrapWithCondition(method = "renderHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemRenderer;renderItemInFirstPerson(F)V"))
    private boolean iris$disableVanillaRenderHand(ItemRenderer instance, float itemstack) {
        return !CeleritasShadersApi.getInstance().isShaderPackInUse();
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;clipRenderersByFrustum(Lnet/minecraft/client/renderer/culling/ICamera;F)V", shift = At.Shift.AFTER), method = "renderWorld(FJ)V")
    private void iris$beginEntities(float partialTicks, long startTime, CallbackInfo ci) {
        final Camera camera = new Camera(mc.renderViewEntity, partialTicks);
        pipeline.renderShadows(this, camera);
    }


    @Redirect(method = "renderWorld(FJ)V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/settings/GameSettings;renderDistanceChunks:I")    )
    /*slice = @Slice(from = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;clear(IZ)V"))*/
    private int iris$alwaysRenderSky(GameSettings instance) {
        return Math.max(instance.renderDistanceChunks, 4);
    }

    @Inject(method = "renderWorld(FJ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;renderSky(F)V"))
    private void iris$beginSky(float partialTicks, long startTime, CallbackInfo ci) {
        // Use CUSTOM_SKY until levelFogColor is called as a heuristic to catch FabricSkyboxes.
        pipeline.setPhase(WorldRenderingPhase.CUSTOM_SKY);
    }

    @Inject(method = "renderWorld(FJ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;renderSky(F)V", shift = At.Shift.AFTER))
    private void iris$endSky(float partialTicks, long startTime, CallbackInfo ci) {
        pipeline.setPhase(WorldRenderingPhase.NONE);
    }

    @WrapOperation(method = "renderWorld(FJ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;renderCloudsCheck(Lnet/minecraft/client/renderer/RenderGlobal;F)V"))
    private void iris$clouds(EntityRenderer instance, RenderGlobal rg, float partialTicks, Operation<Void> original) {
        pipeline.setPhase(WorldRenderingPhase.CLOUDS);
        original.call(instance, rg, partialTicks);
        pipeline.setPhase(WorldRenderingPhase.NONE);
    }


    @Inject(method = "renderWorld(FJ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;renderRainSnow(F)V"))
    private void iris$beginWeatherAndwriteRainAndSnowToDepthBuffer(float partialTicks, long startTime, CallbackInfo ci) {
        pipeline.setPhase(WorldRenderingPhase.RAIN_SNOW);
        if (pipeline.shouldWriteRainAndSnowToDepthBuffer()) {
            GL11.glDepthMask(true);
        }
    }

    @Inject(method = "renderWorld(FJ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;renderRainSnow(F)V", shift = At.Shift.AFTER))
    private void iris$endWeather(float partialTicks, long startTime, CallbackInfo ci) {
        pipeline.setPhase(WorldRenderingPhase.NONE);
    }

    @WrapOperation(method = "renderWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;drawBlockDamageTexture(Lnet/minecraft/client/renderer/Tessellator;Lnet/minecraft/entity/EntityLivingBase;F)V", remap = false))
    private void iris$blockDamageTexture(RenderGlobal instance, Tessellator tessellator, EntityLivingBase entity, float partialTicks, Operation<Void> original) {
        pipeline.setPhase(WorldRenderingPhase.DESTROY);
        original.call(instance, tessellator, entity, partialTicks);
        pipeline.setPhase(WorldRenderingPhase.NONE);
    }
}
