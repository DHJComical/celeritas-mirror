package org.taumc.celeritas.render.terrain;

import org.embeddedt.embeddium.impl.gl.device.RenderDevice;
import org.embeddedt.embeddium.impl.render.chunk.DefaultChunkRenderer;
import org.embeddedt.embeddium.impl.render.chunk.RenderPassConfiguration;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderInterface;

public class PostmodernChunkRenderer extends DefaultChunkRenderer {
    public PostmodernChunkRenderer(RenderDevice device, RenderPassConfiguration<?> renderPassConfiguration) {
        super(device, renderPassConfiguration);
    }

    @Override
    protected void configureShaderInterface(ChunkShaderInterface shader) {

    }
}
