package net.irisshaders.iris.mixin.vertices;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.irisshaders.iris.vertices.BlockSensitiveBufferBuilder;
import net.irisshaders.iris.vertices.ExtendedDataHelper;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.embeddedt.embeddium.impl.util.WorldUtil;
import org.spongepowered.asm.mixin.Mixin;

/**
 * The fluid counterpart of {@link MixinModelBlockRenderer}.
 */
@Mixin(BlockRenderDispatcher.class)
public class MixinBlockRenderDispatcher {
	@WrapMethod(method = "renderLiquid")
	private void iris$beginFluid(BlockPos pos, BlockAndTintGetter level, VertexConsumer consumer, BlockState blockState,
								 FluidState fluidState, Operation<Void> original) {
		if (!(consumer instanceof BlockSensitiveBufferBuilder blockSensitive)) {
			original.call(pos, level, consumer, blockState, fluidState);
			return;
		}

		var blockStateIds = WorldRenderingSettings.INSTANCE.getBlockStateIds();
		short id = blockStateIds == null ? -1 : (short) blockStateIds.getOrDefault(fluidState.createLegacyBlock(), -1);

		blockSensitive.beginBlock(id, ExtendedDataHelper.FLUID_RENDER_TYPE, (byte) WorldUtil.getLightEmission(blockState, level, pos),
			pos.getX(), pos.getY(), pos.getZ());

		try {
			original.call(pos, level, consumer, blockState, fluidState);
		} finally {
			blockSensitive.endBlock();
		}
	}
}
