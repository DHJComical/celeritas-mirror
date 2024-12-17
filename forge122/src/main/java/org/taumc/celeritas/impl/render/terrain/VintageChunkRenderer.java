package org.taumc.celeritas.impl.render.terrain;

import org.embeddedt.embeddium.impl.gl.device.RenderDevice;
import org.embeddedt.embeddium.impl.render.chunk.DefaultChunkRenderer;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexType;

public class VintageChunkRenderer extends DefaultChunkRenderer {
    public VintageChunkRenderer(RenderDevice device, ChunkVertexType vertexType) {
        super(device, vertexType);
        this.useBlockFaceCulling = true;
    }
}
