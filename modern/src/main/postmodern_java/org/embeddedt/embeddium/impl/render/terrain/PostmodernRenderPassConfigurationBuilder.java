package org.embeddedt.embeddium.impl.render.terrain;

import com.mojang.blaze3d.opengl.GlSampler;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.opengl.GlTextureView;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.data.AtlasIds;
import org.embeddedt.embeddium.impl.blaze3d.CeleritasCommandEncoder;
import org.embeddedt.embeddium.impl.mixin.core.GlCommandEncoderMixin;
import org.embeddedt.embeddium.impl.render.ShaderModBridge;
import org.embeddedt.embeddium.impl.render.chunk.RenderPassConfiguration;
import org.embeddedt.embeddium.impl.render.chunk.compile.sorting.QuadPrimitiveType;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.parameters.AlphaCutoffParameter;
import org.embeddedt.embeddium.impl.render.chunk.terrain.sampling.TerrainSampling;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexType;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.opengl.GL33C;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class PostmodernRenderPassConfigurationBuilder {
    private static TerrainSampling createTerrainSampling() {
        if (ShaderModBridge.areShadersEnabled()) {
            return TerrainSampling.NATIVE;
        }
        var filter = Minecraft.getInstance().options.textureFiltering().get() == TextureFilteringMethod.RGSS
                ? TerrainSampling.Filter.RGSS
                : TerrainSampling.Filter.NEAREST_EMULATED;
        var atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS);
        int width = atlas.getTexture().getWidth(0);
        // The stitcher pads every sprite by the same amount
        var missingSprite = atlas.missingSprite();
        int padding = Math.round(missingSprite.getU0() * width) - missingSprite.getX();
        return new TerrainSampling.Padded(filter, width, atlas.getTexture().getHeight(0), padding);
    }

    private static Optional<Float> getAlphaCutoff(ChunkSectionLayer layer) {
        return Optional.ofNullable(layer.pipeline().getShaderDefines().values().get("ALPHA_CUTOUT")).map(Float::parseFloat);
    }

    private record VanillaTerrainConfig(@Nullable ChunkSectionLayerGroup group, RenderPipeline pipeline, boolean hasAlphaCutoff, boolean translucencySorted) {
        private static @Nullable ChunkSectionLayerGroup getGroup(ChunkSectionLayer layer) {
            for (ChunkSectionLayerGroup g : ChunkSectionLayerGroup.values()) {
                for (ChunkSectionLayer other : g.layers()) {
                    if (other == layer) {
                        return g;
                    }
                }
            }
            return null;
        }

        private static VanillaTerrainConfig fromLayer(ChunkSectionLayer layer) {
            boolean hasAlphaCutoff = getAlphaCutoff(layer).orElse(0f) > 0;
            return new VanillaTerrainConfig(getGroup(layer), layer.pipeline(), hasAlphaCutoff, layer.translucent());
        }

        // Resolved for every pass, since both the main and the translucent target can change between frames.
        private RenderTarget target() {
            return group != null ? group.outputTarget() : Minecraft.getInstance().getMainRenderTarget();
        }
    }

    public record PostmodernPipeline(String name, VanillaTerrainConfig config, boolean filtered) implements TerrainRenderPass.PipelineState {
        private static RenderPass currentChunkRenderPass;
        public static GpuSampler terrainSampler;
        /**
         * The sampler vanilla would draw terrain with, built from the texture filtering options. Set before each
         * terrain group is drawn.
         */
        public static @Nullable GpuSampler vanillaTerrainSampler;

        @Override
        public void setup() {
            if (currentChunkRenderPass != null) {
                throw new IllegalStateException("In previous render pass");
            }
            if (terrainSampler == null) {
                terrainSampler = RenderSystem.getDevice().createSampler(
                        AddressMode.CLAMP_TO_EDGE,
                        AddressMode.CLAMP_TO_EDGE,
                        FilterMode.NEAREST, FilterMode.NEAREST,
                        1, OptionalDouble.empty());
            }
            var encoder = RenderSystem.getDevice().createCommandEncoder();
            var rendertarget = config.target();
            currentChunkRenderPass = encoder
                    .createRenderPass(
                            () -> "Section layers for " + name,
                            rendertarget.getColorTextureView(),
                            OptionalInt.empty(),
                            rendertarget.getDepthTextureView(),
                            OptionalDouble.empty()
                    );
            if (encoder instanceof CeleritasCommandEncoder glEncoder) {
                glEncoder.celeritas$configureForPipeline(config.pipeline());
            } else {
                throw new IllegalStateException("Unexpected command encoder class: " + encoder.getClass().getName());
            }
            GlStateManager._activeTexture(GL32C.GL_TEXTURE0);
            // We use our own sampler for shaderpacks
            var atlasSampler = filtered && vanillaTerrainSampler != null ? vanillaTerrainSampler : terrainSampler;
            bindAndApplyConfig(Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getTextureView(), 0, atlasSampler);
            GlStateManager._activeTexture(GL32C.GL_TEXTURE2);
            bindAndApplyConfig(Minecraft.getInstance().gameRenderer.lightmap(), 2, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            GlStateManager._activeTexture(GL32C.GL_TEXTURE0);
        }

        private static void bindAndApplyConfig(GpuTextureView view, int id, GpuSampler sampler) {
            if (view instanceof GlTextureView glView) {
                GlStateManager._bindTexture(glView.texture().glId());
            } else {
                throw new IllegalStateException("Unexpected texture view class: " + view.getClass().getName());
            }
            if (sampler instanceof GlSampler glSampler) {
                GL33C.glBindSampler(id, glSampler.getId());
            } else {
                throw new IllegalStateException();
            }
            int j = 3553;
            GlStateManager._texParameter(j, 33084, view.baseMipLevel());
            GlStateManager._texParameter(j, 33085, view.baseMipLevel() + view.mipLevels() - 1);
        }

        @Override
        public void clear() {
            GlStateManager._activeTexture(GL32C.GL_TEXTURE0);
            GlStateManager._bindTexture(0);
            GL33C.glBindSampler(0, 0);
            GlStateManager._activeTexture(GL32C.GL_TEXTURE2);
            GlStateManager._bindTexture(0);
            GL33C.glBindSampler(2, 0);
            GlStateManager._activeTexture(GL32C.GL_TEXTURE0);
            Objects.requireNonNull(currentChunkRenderPass);
            currentChunkRenderPass.close();
            currentChunkRenderPass = null;
            // Terrain is drawn with Celeritas's own programs.
            ((CeleritasCommandEncoder) RenderSystem.getDevice().createCommandEncoder()).celeritas$invalidateAppliedState();
        }
    }

    public static RenderPassConfiguration<ChunkSectionLayer> createRenderPassConfiguration(ChunkVertexType vertexType) {
        Map<ChunkSectionLayer, Material> materialMap = new EnumMap<>(ChunkSectionLayer.class);
        Map<ChunkSectionLayer, Collection<TerrainRenderPass>> renderPasses = new EnumMap<>(ChunkSectionLayer.class);
        Map<VanillaTerrainConfig, TerrainRenderPass> vanillaPasses = new HashMap<>();
        Map<String, String> extraDefines = new HashMap<>();
        int fadeInMillis = (int) Math.round(Minecraft.getInstance().options.chunkSectionFadeInTime().get() * 1000.0);
        if (fadeInMillis > 0) {
            extraDefines.put("CHUNK_FADE_IN_DURATION_MS", String.valueOf(fadeInMillis));
        }
        AtomicInteger id = new AtomicInteger(1);
        var terrainSampling = createTerrainSampling();
        boolean filtered = !(terrainSampling instanceof TerrainSampling.Native);
        for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
            var config = VanillaTerrainConfig.fromLayer(layer);
            var terrainPass = vanillaPasses.computeIfAbsent(config, c -> {
                var name = "generated_for_" + layer.label();
                var pass = TerrainRenderPass.builder()
                        .name(name)
                        .fragmentDiscard(c.hasAlphaCutoff())
                        .vertexType(vertexType)
                        .primitiveType(QuadPrimitiveType.TRIANGULATED)
                        .useReverseOrder(c.translucencySorted())
                        .pipelineState(new PostmodernPipeline(name, c, filtered))
                        .useTranslucencySorting(c.translucencySorted())
                        .extraDefines(extraDefines)
                        .build();
                renderPasses.computeIfAbsent(layer, $ -> new ArrayList<>()).add(pass);
                return pass;
            });
            var alphaCutoff = getAlphaCutoff(layer).map(AlphaCutoffParameter::valueOf).orElse(AlphaCutoffParameter.ZERO);
            materialMap.put(layer, new Material(terrainPass, alphaCutoff, true));
        }
        renderPasses.replaceAll((layer, list) -> List.copyOf(list));
        return new RenderPassConfiguration<>(materialMap, renderPasses,
                materialMap.get(ChunkSectionLayer.SOLID),
                materialMap.get(ChunkSectionLayer.CUTOUT),
                materialMap.get(ChunkSectionLayer.TRANSLUCENT),
                terrainSampling);
    }
}
