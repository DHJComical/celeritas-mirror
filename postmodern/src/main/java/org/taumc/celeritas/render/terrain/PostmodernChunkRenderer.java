package org.taumc.celeritas.render.terrain;

import org.embeddedt.embeddium.impl.gl.device.RenderDevice;
import org.embeddedt.embeddium.impl.render.chunk.DefaultChunkRenderer;
import org.embeddedt.embeddium.impl.render.chunk.RenderPassConfiguration;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderInterface;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderTextureSlot;

public class PostmodernChunkRenderer extends DefaultChunkRenderer {
    public PostmodernChunkRenderer(RenderDevice device, RenderPassConfiguration<?> renderPassConfiguration) {
        super(device, renderPassConfiguration);
    }

    @Override
    protected void configureShaderInterface(ChunkShaderInterface shader) {
        shader.setTextureSlot(ChunkShaderTextureSlot.BLOCK, 0);
        shader.setTextureSlot(ChunkShaderTextureSlot.LIGHT, 2);
    }
}
