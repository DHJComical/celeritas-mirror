package org.embeddedt.embeddium.impl.render.chunk;

import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexType;

import java.util.List;

public record RenderPassConfiguration(ChunkVertexType vertexType, List<TerrainRenderPass> renderPasses) {
}
