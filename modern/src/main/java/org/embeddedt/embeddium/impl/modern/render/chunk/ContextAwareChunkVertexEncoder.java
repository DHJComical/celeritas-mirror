package org.embeddedt.embeddium.impl.modern.render.chunk;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.embeddedt.embeddium.impl.modern.render.chunk.compile.pipeline.BlockRenderContext;
import org.jetbrains.annotations.Nullable;

public interface ContextAwareChunkVertexEncoder {
    void prepareToRenderBlockFace(BlockRenderContext ctx, @Nullable Direction side);
    void prepareToRenderFluidFace(BlockRenderContext ctx);
    void prepareToVoxelizeLight(BlockState state);
    void finishRenderingBlock();
}
