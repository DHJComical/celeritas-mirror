package net.irisshaders.iris.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * This mixin makes the effects of view bobbing and nausea apply to the model view matrix, not the projection matrix.
 * <p>
 * Applying these effects to the projection matrix causes severe issues with most shaderpacks. As it turns out, OptiFine
 * applies these effects to the modelview matrix. As such, we must do the same to properly run shaderpacks.
 * <p>
 * This mixin makes use of the matrix stack in order to make these changes without more invasive changes.
 */
@Mixin(GameRenderer.class)
public abstract class MixinModelViewBobbing {
    //? if <1.20.6 {
	@Unique
	private Matrix4f bobbingEffectsModel;

	@Unique
	private boolean areShadersOn;

	@Inject(method = "renderLevel", at = @At("HEAD"))
	private void iris$saveShadersOn(float pGameRenderer0, long pLong1, PoseStack pPoseStack2, CallbackInfo ci) {
		areShadersOn = IrisApi.getInstance().isShaderPackInUse();
	}

	@ModifyArg(method = "renderLevel", index = 0,
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/GameRenderer;bobHurt(Lcom/mojang/blaze3d/vertex/PoseStack;F)V"))
	private PoseStack iris$separateViewBobbing(PoseStack stack) {
		if (!areShadersOn) return stack;

		stack.pushPose();
		stack.last().pose().identity();

		return stack;
	}

	@Redirect(method = "renderLevel",
		at = @At(value = "INVOKE",
			target = "Lcom/mojang/blaze3d/vertex/PoseStack;last()Lcom/mojang/blaze3d/vertex/PoseStack$Pose;"),
		slice = @Slice(from = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/GameRenderer;bobHurt(Lcom/mojang/blaze3d/vertex/PoseStack;F)V"), to = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;resetProjectionMatrix(Lorg/joml/Matrix4f;)V")))
	private PoseStack.Pose iris$saveBobbing(PoseStack stack) {
		if (!areShadersOn) return stack.last();

		bobbingEffectsModel = new Matrix4f(stack.last().pose());

		stack.popPose();

		return stack.last();
	}

	@Inject(method = "renderLevel",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/GameRenderer;resetProjectionMatrix(Lorg/joml/Matrix4f;)V"))
	private void iris$applyBobbingToModelView(float tickDelta, long limitTime, PoseStack matrix, CallbackInfo ci) {
		if (!areShadersOn) return;

		matrix.last().pose().mul(bobbingEffectsModel);

		bobbingEffectsModel = null;
	}
    //?}
    //? if >=1.20.6 {
    /*@Shadow
    protected abstract void bobView(PoseStack pGameRenderer0, float pFloat1);

    @Shadow
    protected abstract void bobHurt(PoseStack pGameRenderer0, float pFloat1);

    @Shadow
    @Final
    private Minecraft minecraft;

    @Unique
    private boolean areShadersOn;

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void iris$saveShadersOn(float pGameRenderer0, long pLong1, CallbackInfo ci) {
        areShadersOn = IrisApi.getInstance().isShaderPackInUse();
    }

    @ModifyArg(method = "renderLevel", index = 0,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/GameRenderer;bobHurt(Lcom/mojang/blaze3d/vertex/PoseStack;F)V"))
    private PoseStack iris$separateViewBobbing(PoseStack stack) {
        if (!areShadersOn) return stack;

        stack.pushPose();
        stack.last().pose().identity();

        return stack;
    }

    @Redirect(method = "renderLevel",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/GameRenderer;bobView(Lcom/mojang/blaze3d/vertex/PoseStack;F)V"))
    private void iris$stopBobbing(GameRenderer instance, PoseStack pGameRenderer0, float pFloat1) {
        if (!areShadersOn) this.bobView(pGameRenderer0, pFloat1);
    }


    @Redirect(method = "renderLevel",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/GameRenderer;bobHurt(Lcom/mojang/blaze3d/vertex/PoseStack;F)V"))
    private void iris$saveBobbing(GameRenderer instance, PoseStack pGameRenderer0, float pFloat1) {
        if (!areShadersOn) this.bobHurt(pGameRenderer0, pFloat1);
    }

    @Redirect(method = "renderLevel",
            at = @At(value = "INVOKE",
                    target = "Lorg/joml/Matrix4f;rotationXYZ(FFF)Lorg/joml/Matrix4f;"))
    private Matrix4f iris$applyBobbingToModelView(Matrix4f instance, float angleX, float angleY, float angleZ, float tickDelta) {
        if (!areShadersOn) {
            instance.rotateXYZ(angleX, angleY, angleZ);

            return instance;
        }

        PoseStack stack = new PoseStack();
        stack.last().pose().set(instance);

        this.bobHurt(stack, tickDelta);
        if (this.minecraft.options.bobView().get()) {
            this.bobView(stack, tickDelta);
        }

        instance.set(stack.last().pose());
        instance.rotateXYZ(angleX, angleY, angleZ);

        return instance;
    }
    *///?}
}
