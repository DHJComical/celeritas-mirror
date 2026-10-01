package org.taumc.celeritas.impl.render.terrain.compile;

import lombok.Getter;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.util.BlockRenderLayer;

/**
 * The vanilla buffer handed to {@link net.minecraft.client.renderer.BlockRendererDispatcher} during meshing. It lets
 * our dispatcher hook recognize calls made on behalf of a chunk build and find that build's context.
 */
@Getter
public class ChunkBufferBuilder extends BufferBuilder {
    private final VintageChunkBuildContext context;
    private final BlockRenderLayer layer;

    public ChunkBufferBuilder(int bufferSize, VintageChunkBuildContext context, BlockRenderLayer layer) {
        super(bufferSize);
        this.context = context;
        this.layer = layer;
    }
}
