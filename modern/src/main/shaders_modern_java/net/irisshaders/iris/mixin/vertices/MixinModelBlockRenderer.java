package net.irisshaders.iris.mixin.vertices;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.irisshaders.iris.vertices.BlockSensitiveBufferBuilder;
import net.irisshaders.iris.vertices.ExtendedDataHelper;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
//? if forge
import net.minecraftforge.client.model.data.ModelData;
//? if neoforge
//import net.neoforged.neoforge.client.model.data.ModelData;
import org.embeddedt.embeddium.impl.util.WorldUtil;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Tells block-sensitive consumers which block they receive, so block models tesselated outside of chunk meshing
 * (Flywheel models, falling blocks) carry the block's ID and light emission.
 */
@Mixin(ModelBlockRenderer.class)
public class MixinModelBlockRenderer {
	@WrapMethod(method = "tesselateBlock(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZLnet/minecraft/util/RandomSource;JI" +
		/*? if forge {*/ "Lnet/minecraftforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;" + /*?}*/
		/*? if neoforge {*/ /*"Lnet/neoforged/neoforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;" + *//*?}*/
		")V"/*? if forgelike {*/, remap = false/*?}*/)
	private void iris$beginBlock(BlockAndTintGetter level, BakedModel model, BlockState state, BlockPos pos, PoseStack poseStack,
								 VertexConsumer consumer, boolean checkSides, RandomSource random, long seed, int packedOverlay,
								 /*? if forgelike {*/ModelData modelData, RenderType renderType, /*?}*/Operation<Void> original) {
		if (!(consumer instanceof BlockSensitiveBufferBuilder blockSensitive)) {
			original.call(level, model, state, pos, poseStack, consumer, checkSides, random, seed, packedOverlay/*? if forgelike {*/, modelData, renderType/*?}*/);
			return;
		}

		var blockStateIds = WorldRenderingSettings.INSTANCE.getBlockStateIds();
		short id = blockStateIds == null ? -1 : (short) blockStateIds.getOrDefault(state, -1);

		blockSensitive.beginBlock(id, ExtendedDataHelper.BLOCK_RENDER_TYPE, (byte) WorldUtil.getLightEmission(state, level, pos),
			pos.getX(), pos.getY(), pos.getZ());

		try {
			original.call(level, model, state, pos, poseStack, consumer, checkSides, random, seed, packedOverlay/*? if forgelike {*/, modelData, renderType/*?}*/);
		} finally {
			blockSensitive.endBlock();
		}
	}
}
