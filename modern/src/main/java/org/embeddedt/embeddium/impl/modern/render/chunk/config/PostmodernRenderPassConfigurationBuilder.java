package org.embeddedt.embeddium.impl.modern.render.chunk.config;

//? if >=1.21.5 {

/*import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import org.embeddedt.embeddium.impl.render.chunk.RenderPassConfiguration;
import org.embeddedt.embeddium.impl.render.chunk.compile.sorting.QuadPrimitiveType;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.parameters.AlphaCutoffParameter;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexType;

import java.util.*;

public class PostmodernRenderPassConfigurationBuilder {
    private record PostmodernPipelineState(RenderPipeline vanillaPipeline) implements TerrainRenderPass.PipelineState {
        @Override
        public void setup() {

        }

        @Override
        public void clear() {

        }
    }

    public static RenderPassConfiguration<ChunkSectionLayer> build(ChunkVertexType vertexType) {
        Map<ChunkSectionLayer, Material> layerToMaterial = new EnumMap<>(ChunkSectionLayer.class);
        Map<ChunkSectionLayer, Collection<TerrainRenderPass>> renderStages = new EnumMap<>(ChunkSectionLayer.class);
        for (var layer : ChunkSectionLayer.values()) {
            var pipeline = layer.pipeline();
            var alphaCutoff = Optional.ofNullable(pipeline.getShaderDefines().values().get("ALPHA_CUTOUT")).map(Float::parseFloat);
            var terrainPass = TerrainRenderPass.builder().name(layer.name().toLowerCase(Locale.ROOT))
                    .pipelineState(new PostmodernPipelineState(pipeline))
                    .vertexType(vertexType)
                    .primitiveType(QuadPrimitiveType.TRIANGULATED)
                    .fragmentDiscard(alphaCutoff.isPresent())
                    .useReverseOrder(layer.sortOnUpload())
                    .useTranslucencySorting(layer.sortOnUpload())
                    .build();
            var material = new Material(terrainPass, alphaCutoff.map(AlphaCutoffParameter::valueOf).orElse(AlphaCutoffParameter.ZERO), layer.useMipmaps);
            layerToMaterial.put(layer, material);
            renderStages.put(layer, List.of(terrainPass));
        }
        return new RenderPassConfiguration<>(layerToMaterial, renderStages,
                layerToMaterial.get(ChunkSectionLayer.SOLID),
                layerToMaterial.get(ChunkSectionLayer.CUTOUT_MIPPED),
                layerToMaterial.get(ChunkSectionLayer.TRANSLUCENT));
    }
}
*///?}
