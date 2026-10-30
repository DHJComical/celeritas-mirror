package net.irisshaders.iris.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.vertex.PoseStack;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.layer.RenderContextIds;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(EntityRenderDispatcher.class)
public class MixinEntityRenderDispatcher {
	// Shader packs with a shadow map draw entity shadows themselves.
	@WrapWithCondition(method = "submit", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitShadow(Lcom/mojang/blaze3d/vertex/PoseStack;FLjava/util/List;)V"))
	private boolean iris$maybeSuppressShadow(SubmitNodeCollector collector, PoseStack poseStack, float radius, List<EntityRenderState.ShadowPiece> pieces) {
		WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();

		return pipeline == null || !pipeline.shouldDisableVanillaEntityShadows();
	}

	// After the push, since by then the entity is being drawn.
	@Inject(method = "submit", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = At.Shift.AFTER))
	private void iris$beginEntity(CallbackInfo ci, @Local(argsOnly = true) EntityRenderState state) {
		RenderContextIds.setEntity(state);
	}

	@Inject(method = "submit", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V"))
	private void iris$endEntity(CallbackInfo ci) {
		RenderContextIds.reset();
	}
}
