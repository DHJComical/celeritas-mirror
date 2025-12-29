package org.taumc.celeritas.render.terrain;

import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import org.embeddedt.embeddium.impl.render.chunk.RenderPassConfiguration;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildContext;

public class PostmodernChunkBuildContext extends ChunkBuildContext {
    public PostmodernChunkBuildContext(RenderPassConfiguration<ChunkSectionLayer> renderPassConfiguration) {
        super(renderPassConfiguration);
    }
}
