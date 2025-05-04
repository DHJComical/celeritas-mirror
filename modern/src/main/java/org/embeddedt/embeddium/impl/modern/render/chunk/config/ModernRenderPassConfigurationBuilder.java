package org.embeddedt.embeddium.impl.modern.render.chunk.config;

import com.google.common.collect.ImmutableListMultimap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.client.renderer.RenderType;
import org.embeddedt.embeddium.impl.Celeritas;
import org.embeddedt.embeddium.impl.render.chunk.RenderPassConfiguration;
import org.embeddedt.embeddium.impl.render.chunk.compile.sorting.QuadPrimitiveType;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.parameters.AlphaCutoffParameter;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexType;

import java.util.Map;

public class ModernRenderPassConfigurationBuilder {

    private record ModernRenderTypePipelineState(RenderType chunkRenderType) implements TerrainRenderPass.PipelineState {
        @Override
        public void setup() {
            this.chunkRenderType.setupRenderState();
        }

        @Override
        public void clear() {
            this.chunkRenderType.clearRenderState();
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            ModernRenderTypePipelineState that = (ModernRenderTypePipelineState) o;
            return chunkRenderType == that.chunkRenderType;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(chunkRenderType);
        }
    }

    private static TerrainRenderPass.TerrainRenderPassBuilder builderForRenderType(RenderType chunkRenderType) {
        return TerrainRenderPass.builder().pipelineState(new ModernRenderTypePipelineState(chunkRenderType));
    }

    public static RenderPassConfiguration<RenderType> build(ChunkVertexType vertexType) {
        // First, build the main passes
        TerrainRenderPass solidPass, cutoutMippedPass, translucentPass, tripwirePass;

        solidPass = builderForRenderType(RenderType.solid())
                .name("solid")
                .fragmentDiscard(false)
                .useReverseOrder(false)
                .build();
        cutoutMippedPass = builderForRenderType(RenderType.cutoutMipped())
                .name("cutout_mipped")
                .fragmentDiscard(true)
                .useReverseOrder(false)
                .build();
        translucentPass = builderForRenderType(RenderType.translucent())
                .name("translucent")
                .fragmentDiscard(false)
                .useReverseOrder(true)
                .useTranslucencySorting(Celeritas.canApplyTranslucencySorting())
                .build();

        //? if >=1.16 {
        tripwirePass = builderForRenderType(RenderType.tripwire())
                .name("tripwire")
                .fragmentDiscard(true)
                .useReverseOrder(true)
                .build();
        //?}

        ImmutableListMultimap.Builder<RenderType, TerrainRenderPass> vanillaRenderStages = ImmutableListMultimap.builder();

        // Build the materials for the vanilla render passes
        Material solidMaterial, cutoutMaterial, cutoutMippedMaterial, translucentMaterial, tripwireMaterial;
        solidMaterial = new Material(solidPass, AlphaCutoffParameter.ZERO, true);
        translucentMaterial = new Material(translucentPass, AlphaCutoffParameter.ZERO, true);
        cutoutMippedMaterial = new Material(cutoutMippedPass, AlphaCutoffParameter.ONE_TENTH, true);

        vanillaRenderStages.put(RenderType.solid(), solidPass);
        vanillaRenderStages.put(RenderType.translucent(), translucentPass);

        //? if >=1.16 {
        tripwireMaterial = new Material(tripwirePass, AlphaCutoffParameter.ONE_TENTH, false);
        vanillaRenderStages.put(RenderType.tripwire(), tripwirePass);
        //?}

        if(Celeritas.options().performance.useRenderPassConsolidation) {
            cutoutMaterial = new Material(cutoutMippedPass, AlphaCutoffParameter.ONE_TENTH, false);
            // Render cutout immediately after solid geometry
            vanillaRenderStages.put(RenderType.solid(), cutoutMippedPass);
        } else {
            TerrainRenderPass cutoutPass;

            cutoutPass = builderForRenderType(RenderType.cutout())
                    .name("cutout")
                    .fragmentDiscard(true)
                    .useReverseOrder(false)
                    .build();

            cutoutMaterial = new Material(cutoutPass, AlphaCutoffParameter.ONE_TENTH, false);
            vanillaRenderStages.put(RenderType.cutout(), cutoutPass);
            vanillaRenderStages.put(RenderType.cutoutMipped(), cutoutMippedPass);
        }

        // Now build the material map
        Map<RenderType, Material> renderTypeToMaterialMap = new Reference2ReferenceOpenHashMap<>(RenderType.chunkBufferLayers().size(), Reference2ReferenceOpenHashMap.VERY_FAST_LOAD_FACTOR);

        renderTypeToMaterialMap.put(RenderType.solid(), solidMaterial);
        renderTypeToMaterialMap.put(RenderType.cutout(), cutoutMaterial);
        renderTypeToMaterialMap.put(RenderType.cutoutMipped(), cutoutMippedMaterial);
        renderTypeToMaterialMap.put(RenderType.translucent(), translucentMaterial);
        //? if >=1.16
        renderTypeToMaterialMap.put(RenderType.tripwire(), tripwireMaterial);

        for(RenderType type : RenderType.chunkBufferLayers()) {
            if(!renderTypeToMaterialMap.containsKey(type)) {
                Celeritas.logger().warn("RenderType {} is not recognized by Embeddium. Treating it as cutout_mipped.", type);
                renderTypeToMaterialMap.put(type, cutoutMippedMaterial);
            }
        }

        var vanillaRenderStageMap = vanillaRenderStages.build();
        var allPasses = vanillaRenderStageMap.values().stream().distinct().toList();

        return new RenderPassConfiguration<>(vertexType,
                renderTypeToMaterialMap,
                vanillaRenderStageMap.asMap(),
                pass -> QuadPrimitiveType.INSTANCE,
                solidMaterial,
                cutoutMippedMaterial,
                translucentMaterial);
    }
}
