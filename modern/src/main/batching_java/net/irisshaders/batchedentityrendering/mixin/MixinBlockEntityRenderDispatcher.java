package net.irisshaders.batchedentityrendering.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.irisshaders.batchedentityrendering.impl.Groupable;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockEntityRenderDispatcher.class)
public class MixinBlockEntityRenderDispatcher {
	@WrapMethod(method = "render(Lnet/minecraft/world/level/block/entity/BlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V")
	private void batchedentityrendering$group(BlockEntity blockEntity, float tickDelta, PoseStack poseStack,
											  MultiBufferSource bufferSource, Operation<Void> original) {
		Groupable groupable = bufferSource instanceof Groupable candidate && candidate.maybeStartGroup()
			? candidate
			: null;

		try {
			original.call(blockEntity, tickDelta, poseStack, bufferSource);
		} finally {
			if (groupable != null) {
				groupable.endGroup();
			}
		}
	}
}
