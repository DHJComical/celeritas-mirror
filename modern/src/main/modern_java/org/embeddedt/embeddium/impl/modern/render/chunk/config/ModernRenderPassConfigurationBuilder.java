package org.embeddedt.embeddium.impl.modern.render.chunk.config;

import com.google.common.collect.ImmutableListMultimap;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import org.embeddedt.embeddium.impl.Celeritas;
import org.embeddedt.embeddium.impl.modern.render.chunk.ModernChunkRenderer;
import org.embeddedt.embeddium.impl.render.ShaderModBridge;
import org.embeddedt.embeddium.impl.render.chunk.RenderPassConfiguration;
import org.embeddedt.embeddium.impl.render.chunk.compile.sorting.QuadPrimitiveType;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.parameters.AlphaCutoffParameter;
import org.embeddedt.embeddium.impl.render.chunk.terrain.sampling.AtlasEdgeMask;
import org.embeddedt.embeddium.impl.render.chunk.terrain.sampling.TerrainSampling;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexType;
import org.embeddedt.embeddium.impl.render.texture.TextureAtlasExtended;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.opengl.GL33C;

import java.util.HashMap;
import java.util.Map;

public class ModernRenderPassConfigurationBuilder {
    private static int linearAtlasSampler;

    private static int getLinearAtlasSampler() {
        if (linearAtlasSampler == 0) {
            int sampler = GL33C.glGenSamplers();
            GL33C.glSamplerParameteri(sampler, GL33C.GL_TEXTURE_MIN_FILTER, GL33C.GL_LINEAR_MIPMAP_LINEAR);
            GL33C.glSamplerParameteri(sampler, GL33C.GL_TEXTURE_MAG_FILTER, GL33C.GL_LINEAR);
            GL33C.glSamplerParameteri(sampler, GL33C.GL_TEXTURE_WRAP_S, GL33C.GL_CLAMP_TO_EDGE);
            GL33C.glSamplerParameteri(sampler, GL33C.GL_TEXTURE_WRAP_T, GL33C.GL_CLAMP_TO_EDGE);
            linearAtlasSampler = sampler;
        }
        return linearAtlasSampler;
    }

    /**
     * @param edgeMask if not null, the atlas is sampled with LINEAR filtering and clamped using this mask
     */
    private record ModernRenderTypePipelineState(RenderType chunkRenderType, @Nullable AtlasEdgeMask edgeMask) implements TerrainRenderPass.PipelineState {
        @Override
        public void setup() {
            this.chunkRenderType.setupRenderState();
            //? if >=1.17 {
            int i = GlStateManager._getActiveTexture();

            RenderSystem.activeTexture(GL32C.GL_TEXTURE0 + 0);
            RenderSystem.bindTexture(RenderSystem.getShaderTexture(0));
            RenderSystem.activeTexture(GL32C.GL_TEXTURE0 + 2);
            RenderSystem.bindTexture(RenderSystem.getShaderTexture(2));

            if (this.edgeMask != null) {
                GL33C.glBindSampler(0, getLinearAtlasSampler());

                RenderSystem.activeTexture(GL32C.GL_TEXTURE0 + ModernChunkRenderer.ATLAS_EDGE_MASK_TEXTURE_UNIT);
                RenderSystem.bindTexture(this.edgeMask.handle());
                this.edgeMask.upload();
                GL33C.glBindSampler(ModernChunkRenderer.ATLAS_EDGE_MASK_TEXTURE_UNIT, 0);
            }

            GlStateManager._activeTexture(i);
            //?}
        }

        @Override
        public void clear() {
            //? if >=1.17 {
            int i = GlStateManager._getActiveTexture();
            RenderSystem.activeTexture(GL32C.GL_TEXTURE0 + 0);
            RenderSystem.bindTexture(0);
            RenderSystem.activeTexture(GL32C.GL_TEXTURE0 + 2);
            RenderSystem.bindTexture(0);
            if (this.edgeMask != null) {
                GL33C.glBindSampler(0, 0);
                RenderSystem.activeTexture(GL32C.GL_TEXTURE0 + ModernChunkRenderer.ATLAS_EDGE_MASK_TEXTURE_UNIT);
                RenderSystem.bindTexture(0);
            }
            GlStateManager._activeTexture(i);
            //?}
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

    private static TerrainSampling createTerrainSampling() {
        if (!Celeritas.options().quality.useRgssTextureFiltering || ShaderModBridge.areShadersEnabled()) {
            return TerrainSampling.NATIVE;
        }

        var atlas = (TextureAtlasExtended) Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS);
        var mask = AtlasEdgeMask.create(atlas.celeritas$getWidth(), atlas.celeritas$getHeight(), atlas.celeritas$getMipLevel(), atlas.celeritas$getSpriteRects());

        if (mask == null) {
            return TerrainSampling.NATIVE;
        }

        return new TerrainSampling.EdgeMasked(TerrainSampling.Filter.RGSS, mask);
    }

    private static TerrainRenderPass.TerrainRenderPassBuilder builderForRenderType(RenderType chunkRenderType, ChunkVertexType vertexType, @Nullable AtlasEdgeMask edgeMask) {
        var extraDefines = new HashMap<String, String>();

        if (Celeritas.options().quality.chunkFadeInDuration > 0) {
            extraDefines.put("CHUNK_FADE_IN_DURATION_MS", String.valueOf(Celeritas.options().quality.chunkFadeInDuration));
        }

        return TerrainRenderPass.builder().extraDefines(extraDefines).pipelineState(new ModernRenderTypePipelineState(chunkRenderType, edgeMask)).vertexType(vertexType).primitiveType(QuadPrimitiveType.TRIANGULATED);
    }

    public static RenderPassConfiguration<RenderType> build(ChunkVertexType vertexType) {
        var sampling = createTerrainSampling();
        var edgeMask = sampling instanceof TerrainSampling.EdgeMasked masked ? masked.mask() : null;

        // First, build the main passes
        TerrainRenderPass solidPass, cutoutMippedPass, translucentPass, tripwirePass;

        solidPass = builderForRenderType(RenderType.solid(), vertexType, edgeMask)
                .name("solid")
                .fragmentDiscard(false)
                .useReverseOrder(false)
                .build();
        cutoutMippedPass = builderForRenderType(RenderType.cutoutMipped(), vertexType, edgeMask)
                .name("cutout_mipped")
                .fragmentDiscard(true)
                .useReverseOrder(false)
                .build();
        translucentPass = builderForRenderType(RenderType.translucent(), vertexType, edgeMask)
                .name("translucent")
                .fragmentDiscard(false)
                .useReverseOrder(true)
                .useTranslucencySorting(Celeritas.canApplyTranslucencySorting())
                .build();

        //? if >=1.16 {
        tripwirePass = builderForRenderType(RenderType.tripwire(), vertexType, edgeMask)
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

            cutoutPass = builderForRenderType(RenderType.cutout(), vertexType, edgeMask)
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

        return new RenderPassConfiguration<>(renderTypeToMaterialMap,
                vanillaRenderStageMap.asMap(),
                solidMaterial,
                cutoutMippedMaterial,
                translucentMaterial,
                sampling);
    }
}
