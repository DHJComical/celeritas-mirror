package net.irisshaders.iris.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.irisshaders.iris.Iris;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.embeddedt.embeddium.impl.render.CeleritasWorldRenderer;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies view bobbing and the nausea / portal spin to the model view matrix rather than the projection matrix while
 * a shader pack is in use, as OptiFine does and as shader packs expect.
 */
@Mixin(GameRenderer.class)
public class MixinModelViewBobbing {
	@Unique
	private boolean iris$shadersOn;

	@Unique
	private final Matrix4f iris$viewEffects = new Matrix4f();

	@Inject(method = "renderLevel", at = @At("HEAD"))
	private void iris$beginViewEffects(DeltaTracker deltaTracker, CallbackInfo ci) {
		iris$shadersOn = Iris.isPackInUseQuick();
		iris$viewEffects.identity();
	}

	@WrapOperation(method = "renderLevel", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4f;mul(Lorg/joml/Matrix4fc;)Lorg/joml/Matrix4f;"))
	private Matrix4f iris$moveBobbing(Matrix4f projection, Matrix4fc bobbing, Operation<Matrix4f> original) {
		if (!iris$shadersOn) {
			return original.call(projection, bobbing);
		}

		iris$viewEffects.mul(bobbing);
		return projection;
	}

	@WrapOperation(method = "renderLevel", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4f;rotate(FLorg/joml/Vector3fc;)Lorg/joml/Matrix4f;"))
	private Matrix4f iris$moveSpinRotation(Matrix4f projection, float angle, Vector3fc axis, Operation<Matrix4f> original) {
		if (!iris$shadersOn) {
			return original.call(projection, angle, axis);
		}

		iris$viewEffects.rotate(angle, axis);
		return projection;
	}

	@WrapOperation(method = "renderLevel", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4f;scale(FFF)Lorg/joml/Matrix4f;"))
	private Matrix4f iris$moveSpinScale(Matrix4f projection, float x, float y, float z, Operation<Matrix4f> original) {
		if (!iris$shadersOn) {
			return original.call(projection, x, y, z);
		}

		iris$viewEffects.scale(x, y, z);
		return projection;
	}

	@ModifyArg(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/renderer/state/level/CameraRenderState;Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;ZLnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;)V"), index = 4)
	private Matrix4fc iris$applyViewEffects(Matrix4fc modelView) {
		if (!iris$shadersOn) {
			return modelView;
		}

		Matrix4f affected = new Matrix4f(iris$viewEffects).mul(modelView);

		// Terrain takes its pose before this, when the chunks to render are prepared.
		CeleritasWorldRenderer.instance().setCurrentChunkRenderPose(new Matrix4f(affected));

		return affected;
	}
}
