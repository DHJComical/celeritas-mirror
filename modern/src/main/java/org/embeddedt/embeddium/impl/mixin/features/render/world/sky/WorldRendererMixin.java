package org.embeddedt.embeddium.impl.mixin.features.render.world.sky;

//? if >=1.21.2 {
/*import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import net.minecraft.client.renderer.FogParameters;
*///?}
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.LevelRenderer;
//? if >=1.17
import net.minecraft.world.level.material.FogType;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.embeddedt.embeddium.impl.render.ShaderModBridge;
//? if >=1.20 {
import org.joml.Matrix4f;
 //?} else
/*import com.mojang.math.Matrix4f;*/
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class WorldRendererMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    /**
     * <p>Prevents the sky layer from rendering when the fog distance is reduced
     * from the default. This helps prevent situations where the sky can be seen
     * through chunks culled by fog occlusion. This also fixes the vanilla issue
     * <a href="https://bugs.mojang.com/browse/MC-152504">MC-152504</a> since it
     * is also caused by being able to see the sky through invisible chunks.</p>
     * 
     * <p>However, this fix comes with some caveats. When underwater, it becomes 
     * impossible to see the sun, stars, and moon since the sky is not rendered.
     * While this does not exactly match the vanilla game, it is consistent with
     * what Bedrock Edition does, so it can be considered vanilla-style. This is
     * also more "correct" in the sense that underwater fog is applied to chunks
     * outside of water, so the fog should also be covering the sun and sky.</p>
     * 
     * <p>When updating Sodium to new releases of the game, please check for new
     * ways the fog can be reduced in {@link FogRenderer#setupFog(Camera, FogRenderer.FogMode, float, boolean, float)} ()}.</p>
     */
    //? if <1.21.2 {
    @Inject(method = "renderSky", at = @At("HEAD"), cancellable = true)
    private void preRenderSky(
            CallbackInfo ci
            //? if >=1.18
            , @Local(ordinal = 0, argsOnly = true) Camera camera
            ) {
    //?} else {
    /*@Inject(method = "addSkyPass", at = @At("HEAD"), cancellable = true)
    private void preRenderSky(FrameGraphBuilder frameGraphBuilder, Camera camera, float f, FogParameters fogParameters, CallbackInfo ci) {
    *///?}
        if (ShaderModBridge.areShadersEnabled()) {
            return;
        }

        //? if >=1.18.2 {
        // Cancels sky rendering when the camera is submersed underwater.
        // This prevents the sky from being visible through chunks culled by Sodium's fog occlusion.
        // Fixes https://bugs.mojang.com/browse/MC-152504.
        // Credit to bytzo for noticing the change in 1.18.2.
        if (camera.getFluidInCamera() == FogType.WATER) {
            ci.cancel();
        }
        //?} else {
        /*Camera camera = this.minecraft.gameRenderer.getMainCamera();
        Vec3 cameraPosition = camera.getPosition();
        Entity cameraEntity = camera.getEntity();

        boolean isSubmersed = /^? if >=1.17 {^/ camera.getFluidInCamera() != FogType.NONE /^?} else {^/ /^!camera.getFluidInCamera().isEmpty() ^//^?}^/;
        boolean hasBlindness = cameraEntity instanceof LivingEntity && ((LivingEntity) cameraEntity).hasEffect(MobEffects.BLINDNESS);
        //? if >=1.16 {
        boolean dimensionIsFoggy = this.minecraft.level.effects().isFoggyAt(Mth.floor(cameraPosition.x()), Mth.floor(cameraPosition.y()));
        //?} else
        /^boolean dimensionIsFoggy = this.minecraft.level.getDimension().isFoggyAt(Mth.floor(cameraPosition.x()), Mth.floor(cameraPosition.y()));^/
        boolean useThickFog = dimensionIsFoggy || this.minecraft.gui.getBossOverlay().shouldCreateWorldFog();

        if (isSubmersed || hasBlindness || useThickFog) {
            ci.cancel();
        }
        *///?}
    }
}