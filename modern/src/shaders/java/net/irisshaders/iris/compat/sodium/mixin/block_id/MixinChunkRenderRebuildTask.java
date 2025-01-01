package net.irisshaders.iris.compat.sodium.mixin.block_id;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.RenderType;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.embeddedt.embeddium.impl.modern.render.chunk.compile.tasks.ChunkBuilderMeshingTask;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildBuffers;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildContext;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildOutput;
import org.embeddedt.embeddium.impl.render.chunk.compile.buffers.ChunkModelBuilder;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexEncoder;
import org.embeddedt.embeddium.impl.util.task.CancellationToken;
import net.irisshaders.iris.compat.sodium.impl.block_context.ChunkBuildBuffersExt;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.irisshaders.iris.vertices.ExtendedDataHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Passes additional information indirectly to the vertex writer to support the mc_Entity and at_midBlock parts of the vertex format.
 */
@Mixin(ChunkBuilderMeshingTask.class)
public class MixinChunkRenderRebuildTask {
	private final ChunkVertexEncoder.Vertex[] vertices = ChunkVertexEncoder.Vertex.uninitializedQuad();

	@Inject(method = "execute(Lorg/embeddedt/embeddium/impl/render/chunk/compile/ChunkBuildContext;Lorg/embeddedt/embeddium/impl/util/task/CancellationToken;)Lorg/embeddedt/embeddium/impl/render/chunk/compile/ChunkBuildOutput;", at = @At(value = "INVOKE",
		target = "net/minecraft/world/level/block/state/BlockState.getRenderShape()" +
			"Lnet/minecraft/world/level/block/RenderShape;"))
	private void iris$setLocalPos(ChunkBuildContext context,
								  CancellationToken cancellationSource, CallbackInfoReturnable<ChunkBuildOutput> cir,
								  @Local(ordinal = 0) ChunkBuildBuffers buffers,
								  @Local(ordinal = 0) BlockPos.MutableBlockPos relativeBlockPos,
								  @Local(ordinal = 0) BlockState blockState) {

		int relX = relativeBlockPos.getX();
		int relY = relativeBlockPos.getY();
		int relZ = relativeBlockPos.getZ();

		if (WorldRenderingSettings.INSTANCE.shouldVoxelizeLightBlocks() && blockState.getBlock() instanceof LightBlock) {
			var material = buffers.getRenderPassConfiguration().getMaterialForRenderType(RenderType.cutout());
			ChunkModelBuilder buildBuffers = buffers.get(material);
			((ChunkBuildBuffersExt) buffers).iris$setLocalPos(0, 0, 0);
			((ChunkBuildBuffersExt) buffers).iris$ignoreMidBlock(true);
			((ChunkBuildBuffersExt) buffers).iris$setMaterialId(blockState, (short) 0, (byte) blockState.getLightEmission());
			for (int i = 0; i < 4; i++) {
				vertices[i].x = (float) ((relX & 15)) + 0.25f;
				vertices[i].y = (float) ((relY & 15)) + 0.25f;
				vertices[i].z = (float) ((relZ & 15)) + 0.25f;
				vertices[i].u = 0;
				vertices[i].v = 0;
				vertices[i].color = 0;
				vertices[i].light = blockState.getLightEmission() << 4 | blockState.getLightEmission() << 20;
			}
			buildBuffers.getVertexBuffer(ModelQuadFacing.UNASSIGNED).push(vertices, material);
			((ChunkBuildBuffersExt) buffers).iris$ignoreMidBlock(false);
			return;
		}

		if (context.buffers instanceof ChunkBuildBuffersExt) {
			((ChunkBuildBuffersExt) context.buffers).iris$setLocalPos(relX, relY, relZ);
		}
	}

	@Inject(method = "execute(Lorg/embeddedt/embeddium/impl/render/chunk/compile/ChunkBuildContext;Lorg/embeddedt/embeddium/impl/util/task/CancellationToken;)Lorg/embeddedt/embeddium/impl/render/chunk/compile/ChunkBuildOutput;", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/renderer/block/BlockModelShaper;getBlockModel(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/client/resources/model/BakedModel;"))
	private void iris$wrapGetBlockLayer(ChunkBuildContext context,
										CancellationToken cancellationSource, CallbackInfoReturnable<ChunkBuildOutput> cir,
										@Local(ordinal = 0) BlockState blockState) {
		if (context.buffers instanceof ChunkBuildBuffersExt) {
			((ChunkBuildBuffersExt) context.buffers).iris$setMaterialId(blockState, ExtendedDataHelper.BLOCK_RENDER_TYPE, (byte) blockState.getLightEmission());
		}
	}

	@Inject(method = "execute(Lorg/embeddedt/embeddium/impl/render/chunk/compile/ChunkBuildContext;Lorg/embeddedt/embeddium/impl/util/task/CancellationToken;)Lorg/embeddedt/embeddium/impl/render/chunk/compile/ChunkBuildOutput;", at = @At(value = "INVOKE",
		target = "Lorg/embeddedt/embeddium/impl/modern/render/chunk/compile/pipeline/FluidRenderer;render(Lorg/embeddedt/embeddium/impl/modern/render/chunk/compile/pipeline/BlockRenderContext;Lorg/embeddedt/embeddium/impl/render/chunk/compile/ChunkBuildBuffers;)V"), remap = false)
	private void iris$wrapGetFluidLayer(ChunkBuildContext context,
										CancellationToken cancellationSource, CallbackInfoReturnable<ChunkBuildOutput> cir,
										@Local(ordinal = 0) BlockState blockState,
										@Local(ordinal = 0) FluidState fluidState) {
		if (context.buffers instanceof ChunkBuildBuffersExt) {
			((ChunkBuildBuffersExt) context.buffers).iris$setMaterialId(fluidState.createLegacyBlock(), ExtendedDataHelper.FLUID_RENDER_TYPE, (byte) blockState.getLightEmission());
		}
	}

	@Inject(method = "execute(Lorg/embeddedt/embeddium/impl/render/chunk/compile/ChunkBuildContext;Lorg/embeddedt/embeddium/impl/util/task/CancellationToken;)Lorg/embeddedt/embeddium/impl/render/chunk/compile/ChunkBuildOutput;",
		at = @At(value = "INVOKE", target = "Lorg/embeddedt/embeddium/impl/util/WorldUtil;hasBlockEntity(Lnet/minecraft/world/level/block/state/BlockState;)Z"), remap = false)
	private void iris$resetContext(ChunkBuildContext buildContext, CancellationToken cancellationSource, CallbackInfoReturnable<ChunkBuildOutput> cir) {
		if (buildContext.buffers instanceof ChunkBuildBuffersExt) {
			((ChunkBuildBuffersExt) buildContext.buffers).iris$resetBlockContext();
		}
	}
}
