package org.taumc.celeritas.impl.render.terrain;

import com.google.common.collect.ImmutableListMultimap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.BlockRenderLayer;
import org.embeddedt.embeddium.impl.render.chunk.RenderPassConfiguration;
import org.embeddedt.embeddium.impl.render.chunk.compile.sorting.QuadPrimitiveType;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.parameters.AlphaCutoffParameter;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexType;
import org.taumc.celeritas.CeleritasVintage;

import java.util.Locale;
import java.util.Map;

public class VintageRenderPassConfigurationBuilder {

    private static final TerrainRenderPass.PipelineState MIPMAP_CONTROLLED_STATE = new TerrainRenderPass.PipelineState() {
        @Override
        public void setup() {
            // Forcefully reset the mipmap state to the expected value for terrain. Mods sometimes manage to corrupt it.
            boolean mipped = Minecraft.getMinecraft().gameSettings.mipmapLevels > 0;
            Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            ((AbstractTexture) Minecraft.getMinecraft().getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE)).setBlurMipmapDirect(false, mipped);
        }

        @Override
        public void clear() {

        }
    };

    private static TerrainRenderPass.TerrainRenderPassBuilder builderForRenderType(BlockRenderLayer chunkRenderType, ChunkVertexType vertexType) {
        return TerrainRenderPass.builder().pipelineState(MIPMAP_CONTROLLED_STATE).vertexType(vertexType).primitiveType(QuadPrimitiveType.TRIANGULATED);
    }

    public static RenderPassConfiguration<BlockRenderLayer> build(ChunkVertexType vertexType) {
        // First, build the main passes
        TerrainRenderPass solidPass, cutoutMippedPass, translucentPass;

        solidPass = builderForRenderType(BlockRenderLayer.SOLID, vertexType)
                .name("solid")
                .fragmentDiscard(false)
                .useReverseOrder(false)
                .build();
        cutoutMippedPass = builderForRenderType(BlockRenderLayer.CUTOUT_MIPPED, vertexType)
                .name("cutout_mipped")
                .fragmentDiscard(true)
                .useReverseOrder(false)
                .build();
        translucentPass = builderForRenderType(BlockRenderLayer.TRANSLUCENT, vertexType)
                .name("translucent")
                .fragmentDiscard(false)
                .useReverseOrder(true)
                .useTranslucencySorting(CeleritasVintage.options().performance.useTranslucentFaceSorting)
                .build();

        ImmutableListMultimap.Builder<BlockRenderLayer, TerrainRenderPass> vanillaRenderStages = ImmutableListMultimap.builder();

        // Build the materials for the vanilla render passes
        Material solidMaterial, cutoutMaterial, cutoutMippedMaterial, translucentMaterial;
        solidMaterial = new Material(solidPass, AlphaCutoffParameter.ZERO, true);
        translucentMaterial = new Material(translucentPass, AlphaCutoffParameter.ZERO, true);
        cutoutMippedMaterial = new Material(cutoutMippedPass, AlphaCutoffParameter.ONE_TENTH, true);

        vanillaRenderStages.put(BlockRenderLayer.SOLID, solidPass);
        vanillaRenderStages.put(BlockRenderLayer.TRANSLUCENT, translucentPass);

        if (CeleritasVintage.options().performance.useRenderPassConsolidation) {
            cutoutMaterial = new Material(cutoutMippedPass, AlphaCutoffParameter.ONE_TENTH, false);
            vanillaRenderStages.put(BlockRenderLayer.SOLID, cutoutMippedPass);
        } else {
            TerrainRenderPass cutoutPass;

            cutoutPass = builderForRenderType(BlockRenderLayer.CUTOUT, vertexType)
                    .name("cutout")
                    .fragmentDiscard(true)
                    .useReverseOrder(false)
                    .build();

            cutoutMaterial = new Material(cutoutPass, AlphaCutoffParameter.ONE_TENTH, false);
            vanillaRenderStages.put(BlockRenderLayer.CUTOUT, cutoutPass);
            vanillaRenderStages.put(BlockRenderLayer.CUTOUT_MIPPED, cutoutMippedPass);
        }

        // Now build the material map
        Map<BlockRenderLayer, Material> renderTypeToMaterialMap = new Reference2ReferenceOpenHashMap<>(4,
                Reference2ReferenceOpenHashMap.VERY_FAST_LOAD_FACTOR);

        renderTypeToMaterialMap.put(BlockRenderLayer.SOLID, solidMaterial);
        renderTypeToMaterialMap.put(BlockRenderLayer.CUTOUT, cutoutMaterial);
        renderTypeToMaterialMap.put(BlockRenderLayer.CUTOUT_MIPPED, cutoutMippedMaterial);
        renderTypeToMaterialMap.put(BlockRenderLayer.TRANSLUCENT, translucentMaterial);

        for (BlockRenderLayer layer : BlockRenderLayer.values()) {
            if (!renderTypeToMaterialMap.containsKey(layer)) {
                CeleritasVintage.logger().warn("Falling back to cutout-like behavior for custom block render layer '{}'", layer);
                TerrainRenderPass pass = builderForRenderType(layer, vertexType).name(layer.name().toLowerCase(Locale.ROOT)).fragmentDiscard(true).useReverseOrder(false).build();
                Material material = new Material(pass, AlphaCutoffParameter.ONE_TENTH, true);
                vanillaRenderStages.put(layer, pass);
                renderTypeToMaterialMap.put(layer, material);
            }
        }

        var vanillaRenderStageMap = vanillaRenderStages.build();

        return new RenderPassConfiguration<>(renderTypeToMaterialMap,
                vanillaRenderStageMap.asMap(),
                solidMaterial,
                cutoutMippedMaterial,
                translucentMaterial);
    }
}
