package org.embeddedt.embeddium.impl.render.chunk;

import org.embeddedt.embeddium.impl.render.chunk.compile.sorting.ChunkPrimitiveType;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexType;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

public record RenderPassConfiguration<R>(ChunkVertexType vertexType,
                                      Map<R, Material> chunkRenderTypeToMaterialMap,
                                      Map<R, Collection<TerrainRenderPass>> vanillaRenderStages,
                                      Function<TerrainRenderPass, ChunkPrimitiveType> primitiveTypeGetter,
                                      Material defaultSolidMaterial,
                                      Material defaultCutoutMippedMaterial,
                                      Material defaultTranslucentMaterial) {
    public ChunkVertexType getVertexTypeForPass(TerrainRenderPass pass) {
        return this.vertexType;
    }

    public ChunkPrimitiveType getPrimitiveTypeForPass(TerrainRenderPass pass) {
        return this.primitiveTypeGetter.apply(pass);
    }

    public Material getMaterialForRenderType(Object type) {
        return Objects.requireNonNull(chunkRenderTypeToMaterialMap.get(type));
    }

    public Stream<TerrainRenderPass> getAllKnownRenderPasses() {
        return vanillaRenderStages().values().stream().flatMap(Collection::stream).distinct();
    }
}
