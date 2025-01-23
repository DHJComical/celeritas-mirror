package org.embeddedt.embeddium.impl.modern.render.chunk;

import net.minecraft.core.Direction;
import org.embeddedt.embeddium.impl.modern.render.chunk.compile.pipeline.BlockRenderContext;
import org.jetbrains.annotations.Nullable;

public interface ContextAwareChunkVertexEncoder {
    void prepareToRenderBlockFace(BlockRenderContext ctx, @Nullable Direction side);
    void prepareToRenderFluidFace(BlockRenderContext ctx);
    void finishRenderingBlock();
}
