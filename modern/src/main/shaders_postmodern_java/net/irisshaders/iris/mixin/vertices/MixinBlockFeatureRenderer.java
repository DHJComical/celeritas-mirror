package net.irisshaders.iris.mixin.vertices;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.irisshaders.iris.vertices.BlockSensitiveBufferBuilder;
import net.irisshaders.iris.vertices.ExtendedDataHelper;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.feature.BlockFeatureRenderer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.embeddedt.embeddium.impl.util.WorldUtil;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Tells block-sensitive consumers which moving block, such as a piston's or a falling block, their quads belong to,
 * and moves those quads to the layer the shader pack assigns the block.
 */
@Mixin(BlockFeatureRenderer.class)
public class MixinBlockFeatureRenderer {
	@Unique
	@Nullable
	private static BlockState iris$block;
	@Unique
	private static BlockAndTintGetter iris$level;
	@Unique
	private static BlockPos iris$pos;

	@WrapOperation(method = "renderMovingBlockSubmits", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/ModelBlockRenderer;tesselateBlock(Lnet/minecraft/client/renderer/block/BlockQuadOutput;FFFLnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;J)V"))
	private void iris$trackBlock(ModelBlockRenderer renderer, BlockQuadOutput output, float x, float y, float z, BlockAndTintGetter level,
								 BlockPos pos, BlockState state, BlockStateModel model, long seed, Operation<Void> original) {
		iris$block = state;
		iris$level = level;
		iris$pos = pos;

		try {
			original.call(renderer, output, x, y, z, level, pos, state, model, seed);
		} finally {
			iris$block = null;
			iris$level = null;
			iris$pos = null;
		}
	}

	// Vanilla draws a moving block in the solid or translucent features by whether its model has translucent quads.
	// A block the pack moves to another layer has to follow it, since the pack's deferred passes run in between.
	@WrapOperation(method = "renderMovingBlockSubmits", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;hasMaterialFlag(Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
	private boolean iris$choosePassFromLayerOverride(BlockStateModel model, BlockAndTintGetter level, BlockPos pos, BlockState state, int flag,
													 Operation<Boolean> original) {
		var overrides = WorldRenderingSettings.INSTANCE.getBlockTypeIds();
		ChunkSectionLayer override = overrides != null && flag == BakedQuad.FLAG_TRANSLUCENT ? overrides.get(state.getBlock()) : null;

		if (override == null) {
			return original.call(model, level, pos, state, flag);
		}

		return override.translucent();
	}

	@ModifyVariable(method = "putBakedQuad", at = @At("HEAD"), argsOnly = true)
	private static ChunkSectionLayer iris$applyLayerOverride(ChunkSectionLayer layer) {
		BlockState state = iris$block;
		var overrides = WorldRenderingSettings.INSTANCE.getBlockTypeIds();

		if (state == null || overrides == null) {
			return layer;
		}

		ChunkSectionLayer override = overrides.get(state.getBlock());
		return override != null ? override : layer;
	}

	@WrapOperation(method = "putBakedQuad", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;putBakedQuad(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/resources/model/geometry/BakedQuad;Lcom/mojang/blaze3d/vertex/QuadInstance;)V"))
	private static void iris$beginBlock(VertexConsumer consumer, PoseStack.Pose pose, BakedQuad quad, QuadInstance instance, Operation<Void> original) {
		BlockState state = iris$block;

		if (state == null || !(consumer instanceof BlockSensitiveBufferBuilder blockSensitive)) {
			original.call(consumer, pose, quad, instance);
			return;
		}

		var blockStateIds = WorldRenderingSettings.INSTANCE.getBlockStateIds();
		short id = blockStateIds == null ? -1 : (short) blockStateIds.getOrDefault(state, -1);

		blockSensitive.beginBlock(id, ExtendedDataHelper.BLOCK_RENDER_TYPE, (byte) WorldUtil.getLightEmission(state, iris$level, iris$pos),
			iris$pos.getX(), iris$pos.getY(), iris$pos.getZ());

		try {
			original.call(consumer, pose, quad, instance);
		} finally {
			blockSensitive.endBlock();
		}
	}
}
