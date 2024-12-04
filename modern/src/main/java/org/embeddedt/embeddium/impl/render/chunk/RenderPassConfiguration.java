package org.embeddedt.embeddium.impl.render.chunk;

import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexType;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record RenderPassConfiguration<R>(ChunkVertexType vertexType,
                                      List<TerrainRenderPass> renderPasses,
                                      Map<R, Material> chunkRenderTypeToMaterialMap,
                                      Map<R, Collection<TerrainRenderPass>> vanillaRenderStages,
                                      Material defaultSolidMaterial,
                                      Material defaultCutoutMippedMaterial,
                                      Material defaultTranslucentMaterial) {
    public Material getMaterialForRenderType(Object type) {
        return Objects.requireNonNull(chunkRenderTypeToMaterialMap.get(type));
    }
}
