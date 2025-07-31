package org.embeddedt.embeddium.impl.modern.render.chunk.config;

//? if >=1.21.5 {

/*import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.opengl.GlTextureView;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import org.embeddedt.embeddium.impl.Celeritas;
import org.embeddedt.embeddium.impl.mixin.core.render.blaze.GlCommandEncoderAccessor;
import org.embeddedt.embeddium.impl.render.chunk.RenderPassConfiguration;
import org.embeddedt.embeddium.impl.render.chunk.compile.sorting.QuadPrimitiveType;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.parameters.AlphaCutoffParameter;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexType;
import org.lwjgl.opengl.GL32C;

import java.util.*;

public class PostmodernRenderPassConfigurationBuilder {
    private static final Map<ChunkSectionLayer, ChunkSectionLayer> CONSOLIDATION_MAPPINGS = Map.of(
            ChunkSectionLayer.CUTOUT, ChunkSectionLayer.CUTOUT_MIPPED
    );
    private static RenderPass currentChunkRenderPass;

    private final Map<ChunkSectionLayer, Material> layerToMaterial = new EnumMap<>(ChunkSectionLayer.class);
    private final Map<ChunkSectionLayer, TerrainRenderPass> passForLayer = new EnumMap<>(ChunkSectionLayer.class);
    private final Map<ChunkSectionLayer, Collection<TerrainRenderPass>> renderStages = new EnumMap<>(ChunkSectionLayer.class);
    private final Map<ChunkSectionLayer, ChunkSectionLayer> consolidationMap = Celeritas.options().performance.useRenderPassConsolidation ? CONSOLIDATION_MAPPINGS : Map.of();
    private final ChunkVertexType vertexType;

    public PostmodernRenderPassConfigurationBuilder(ChunkVertexType vertexType) {
        this.vertexType = vertexType;
    }

    private TerrainRenderPass buildTerrainRenderPassForLayer(ChunkSectionLayer l) {
        return this.passForLayer.computeIfAbsent(l, layer -> {
            var pipeline = layer.pipeline();
            return TerrainRenderPass.builder().name(layer.name().toLowerCase(Locale.ROOT))
                    .pipelineState(new PostmodernPipelineState(layer, pipeline))
                    .vertexType(vertexType)
                    .primitiveType(QuadPrimitiveType.TRIANGULATED)
                    .fragmentDiscard(pipeline.getShaderDefines().values().containsKey("ALPHA_CUTOUT"))
                    .useReverseOrder(layer.sortOnUpload())
                    .useTranslucencySorting(layer.sortOnUpload())
                    .build();
        });
    }

    public RenderPassConfiguration<ChunkSectionLayer> build() {
        for (var layer : ChunkSectionLayer.values()) {
            var pipeline = layer.pipeline();
            var alphaCutoff = Optional.ofNullable(pipeline.getShaderDefines().values().get("ALPHA_CUTOUT")).map(Float::parseFloat);
            TerrainRenderPass terrainPass = this.buildTerrainRenderPassForLayer(consolidationMap.getOrDefault(layer, layer));
            var material = new Material(terrainPass, alphaCutoff.map(AlphaCutoffParameter::valueOf).orElse(AlphaCutoffParameter.ZERO), layer.useMipmaps);
            layerToMaterial.put(layer, material);
            renderStages.put(layer, List.of(terrainPass));
        }

        return new RenderPassConfiguration<>(layerToMaterial, renderStages,
                layerToMaterial.get(ChunkSectionLayer.SOLID),
                layerToMaterial.get(ChunkSectionLayer.CUTOUT_MIPPED),
                layerToMaterial.get(ChunkSectionLayer.TRANSLUCENT));
    }

    private record PostmodernPipelineState(ChunkSectionLayer layer, RenderPipeline vanillaPipeline) implements TerrainRenderPass.PipelineState {
        @Override
        public void setup() {
            if (currentChunkRenderPass != null) {
                throw new IllegalStateException("In previous render pass");
            }
            var encoder = RenderSystem.getDevice().createCommandEncoder();
            var rendertarget = layer.outputTarget();
            currentChunkRenderPass = encoder
                    .createRenderPass(
                            () -> "Section layers for " + layer.name(),
                            rendertarget.getColorTextureView(),
                            OptionalInt.empty(),
                            rendertarget.getDepthTextureView(),
                            OptionalDouble.empty()
                    );
            if (encoder instanceof GlCommandEncoderAccessor glEncoder) {
                glEncoder.invokeApplyPipelineState(vanillaPipeline);
            } else {
                throw new IllegalStateException("Unexpected command encoder class: " + encoder.getClass().getName());
            }
            GlStateManager._activeTexture(GL32C.GL_TEXTURE0);
            bindAndApplyConfig(layer.textureView());
            GlStateManager._activeTexture(GL32C.GL_TEXTURE2);
            bindAndApplyConfig(Minecraft.getInstance().gameRenderer.lightTexture().getTextureView());
            GlStateManager._activeTexture(GL32C.GL_TEXTURE0);
        }

        private static void bindAndApplyConfig(GpuTextureView view) {
            if (view instanceof GlTextureView glView) {
                GlStateManager._bindTexture(glView.texture().glId());
            } else {
                throw new IllegalStateException("Unexpected texture view class: " + view.getClass().getName());
            }
            int j = 3553;
            GlStateManager._texParameter(j, 33084, view.baseMipLevel());
            GlStateManager._texParameter(j, 33085, view.baseMipLevel() + view.mipLevels() - 1);
            glView.texture().flushModeChanges(j);
        }

        @Override
        public void clear() {
            GlStateManager._activeTexture(GL32C.GL_TEXTURE0);
            GlStateManager._bindTexture(0);
            GlStateManager._activeTexture(GL32C.GL_TEXTURE2);
            GlStateManager._bindTexture(0);
            GlStateManager._activeTexture(GL32C.GL_TEXTURE0);
            Objects.requireNonNull(currentChunkRenderPass);
            currentChunkRenderPass.close();
            currentChunkRenderPass = null;
        }
    }
}
*///?}
