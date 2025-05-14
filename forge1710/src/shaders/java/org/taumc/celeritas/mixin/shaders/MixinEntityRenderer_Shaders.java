package org.taumc.celeritas.mixin.shaders;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
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
import org.lwjgl.opengl.GL43C;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.taumc.celeritas.api.v0.CeleritasShadersApi;
import org.taumc.celeritas.compat.Camera;

@Mixin(EntityRenderer.class)
public class MixinEntityRenderer_Shaders {
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
    private void iris$startFrame(float partialTicks, long startTime, CallbackInfo ci, @Share("pipeline") LocalRef<WorldRenderingPipeline> pipeline) {
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

}
