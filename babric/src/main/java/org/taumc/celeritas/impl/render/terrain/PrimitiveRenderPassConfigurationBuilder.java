package org.taumc.celeritas.impl.render.terrain;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import org.embeddedt.embeddium.impl.render.chunk.RenderPassConfiguration;
import org.embeddedt.embeddium.impl.render.chunk.compile.sorting.QuadPrimitiveType;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.parameters.AlphaCutoffParameter;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkMeshFormats;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexType;
import org.lwjgl.opengl.GL11;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class PrimitiveRenderPassConfigurationBuilder {
    public static final TerrainRenderPass SOLID_PASS, CUTOUT_MIPPED_PASS, TRANSLUCENT_PASS;
    public static final Material SOLID_MATERIAL, CUTOUT_MIPPED_MATERIAL, TRANSLUCENT_MATERIAL;

    private record PrimitivePipelineState(int pass, boolean disableBlend) implements TerrainRenderPass.PipelineState {
        @Override
        public void setup() {
            //? if <1.7 {
            if (pass == 1) {
                // Force translucent pass to use backface culling
                GL11.glEnable(GL11.GL_CULL_FACE);
            }
            //?}
            if (disableBlend) {
                GL11.glDisable(GL11.GL_ALPHA_TEST);
            }
        }

        @Override
        public void clear() {
            if (disableBlend) {
                GL11.glEnable(GL11.GL_ALPHA_TEST);
            }
            //? if <1.7 {
            if (pass == 1) {
                // Disable backface culling again
                GL11.glDisable(GL11.GL_CULL_FACE);
            }
            //?}
        }
    }

    private static TerrainRenderPass.TerrainRenderPassBuilder builderForRenderType(int pass, boolean disableBlend) {
        var builder = TerrainRenderPass.builder();
        builder.pipelineState(new PrimitivePipelineState(pass, disableBlend));
        //? if <1.0.0-beta.8.1
        /*builder.hasNoLightmap(true);*/
        builder.vertexType(ChunkMeshFormats.VANILLA_LIKE).primitiveType(QuadPrimitiveType.TRIANGULATED);
        return builder;
    }

    static {
        SOLID_PASS =  builderForRenderType(0, true)
                .name("solid")
                .fragmentDiscard(false)
                .useReverseOrder(false)
                .build();
        CUTOUT_MIPPED_PASS = builderForRenderType(0, false)
                .name("cutout_mipped")
                .fragmentDiscard(true)
                .useReverseOrder(false)
                .build();
        TRANSLUCENT_PASS = builderForRenderType(1, false)
                .name("translucent")
                .fragmentDiscard(false)
                .useReverseOrder(true)
                .useTranslucencySorting(true) // TODO allow disabling
                .build();
        TRANSLUCENT_MATERIAL = new Material(TRANSLUCENT_PASS, AlphaCutoffParameter.ZERO, true);
        SOLID_MATERIAL = new Material(SOLID_PASS, AlphaCutoffParameter.ZERO, true);
        CUTOUT_MIPPED_MATERIAL = new Material(CUTOUT_MIPPED_PASS, AlphaCutoffParameter.ONE_TENTH, true);
    }

    public static RenderPassConfiguration<Integer> build(ChunkVertexType vertexType) {
        Int2ObjectMap<Collection<TerrainRenderPass>> vanillaRenderStages = new Int2ObjectOpenHashMap<>();

        vanillaRenderStages.put(1, List.of(TRANSLUCENT_PASS));
        vanillaRenderStages.put(0, List.of(SOLID_PASS, CUTOUT_MIPPED_PASS));

        // Now build the material map
        Map<Integer, Material> renderTypeToMaterialMap = new Reference2ReferenceOpenHashMap<>(4,
                Reference2ReferenceOpenHashMap.VERY_FAST_LOAD_FACTOR);

        renderTypeToMaterialMap.put(0, CUTOUT_MIPPED_MATERIAL);
        renderTypeToMaterialMap.put(1, TRANSLUCENT_MATERIAL);

        return new RenderPassConfiguration<>(renderTypeToMaterialMap,
                vanillaRenderStages,
                CUTOUT_MIPPED_MATERIAL,
                CUTOUT_MIPPED_MATERIAL,
                TRANSLUCENT_MATERIAL);
    }
}
