package org.taumc.celeritas.impl.render.terrain;

import com.google.common.collect.ImmutableListMultimap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import org.embeddedt.embeddium.impl.render.chunk.RenderPassConfiguration;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.parameters.AlphaCutoffParameter;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexType;

import java.util.Map;

public class VintageRenderPassConfigurationBuilder {

    private static TerrainRenderPass.TerrainRenderPassBuilder builderForRenderType(int pass) {
        return TerrainRenderPass.builder().setupState(() -> {}).clearState(() -> {});
    }

    public static RenderPassConfiguration<Integer> build(ChunkVertexType vertexType) {
        // First, build the main passes
        TerrainRenderPass cutoutMippedPass, translucentPass;

        cutoutMippedPass = builderForRenderType(0)
                .name("cutout_mipped")
                .fragmentDiscard(true)
                .useReverseOrder(false)
                .build();
        translucentPass = builderForRenderType(1)
                .name("translucent")
                .fragmentDiscard(false)
                .useReverseOrder(true)
                .useTranslucencySorting(true) // TODO allow disabling
                .build();

        ImmutableListMultimap.Builder<Integer, TerrainRenderPass> vanillaRenderStages = ImmutableListMultimap.builder();

        // Build the materials for the vanilla render passes
        Material cutoutMippedMaterial, translucentMaterial;
        translucentMaterial = new Material(translucentPass, AlphaCutoffParameter.ZERO, true);
        cutoutMippedMaterial = new Material(cutoutMippedPass, AlphaCutoffParameter.ONE_TENTH, true);

        vanillaRenderStages.put(1, translucentPass);
        vanillaRenderStages.put(0, cutoutMippedPass);

        // Now build the material map
        Map<Integer, Material> renderTypeToMaterialMap = new Reference2ReferenceOpenHashMap<>(4,
                Reference2ReferenceOpenHashMap.VERY_FAST_LOAD_FACTOR);

        renderTypeToMaterialMap.put(0, cutoutMippedMaterial);
        renderTypeToMaterialMap.put(1, translucentMaterial);

        var vanillaRenderStageMap = vanillaRenderStages.build();
        var allPasses = vanillaRenderStageMap.values().stream().distinct().toList();

        return new RenderPassConfiguration<>(vertexType,
                allPasses,
                renderTypeToMaterialMap,
                vanillaRenderStageMap.asMap(),
                cutoutMippedMaterial,
                cutoutMippedMaterial,
                translucentMaterial);
    }
}
