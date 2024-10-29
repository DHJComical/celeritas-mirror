package org.embeddedt.embeddium.impl.render.chunk;

import net.minecraft.client.renderer.RenderType;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexType;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record RenderPassConfiguration(ChunkVertexType vertexType,
                                      List<TerrainRenderPass> renderPasses,
                                      Map<RenderType, Material> chunkRenderTypeToMaterialMap,
                                      Map<RenderType, Collection<TerrainRenderPass>> vanillaRenderStages,
                                      Material defaultSolidMaterial,
                                      Material defaultCutoutMippedMaterial,
                                      Material defaultTranslucentMaterial) {
    public Material getMaterialForRenderType(RenderType type) {
        return Objects.requireNonNull(chunkRenderTypeToMaterialMap.get(type));
    }

    public Material getMaterialByChunkLayerId(int id) {
        return chunkRenderTypeToMaterialMap.get(RenderType.chunkBufferLayers().get(id));
    }
}
