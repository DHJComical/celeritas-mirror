package org.embeddedt.embeddium.impl.render.chunk;

import net.minecraft.client.renderer.RenderType;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexType;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public record RenderPassConfiguration(ChunkVertexType vertexType,
                                      List<TerrainRenderPass> renderPasses,
                                      List<Material> chunkRenderTypeToMaterialMap,
                                      Map<RenderType, Collection<TerrainRenderPass>> vanillaRenderStages,
                                      Material defaultSolidMaterial,
                                      Material defaultCutoutMippedMaterial,
                                      Material defaultTranslucentMaterial) {
    public Material getMaterialForRenderType(RenderType type) {
        int id = type.getChunkLayerId();
        if (id < 0) {
            throw new IllegalStateException("RenderType " + type + " is not a chunk render type");
        }
        return chunkRenderTypeToMaterialMap.get(id);
    }

    public Material getMaterialByChunkLayerId(int id) {
        return chunkRenderTypeToMaterialMap.get(id);
    }
}
