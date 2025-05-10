package org.taumc.celeritas.impl.render.terrain.compile;

import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.embeddedt.embeddium.impl.render.chunk.RenderPassConfiguration;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildBuffers;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildContext;
import org.embeddedt.embeddium.impl.render.chunk.compile.buffers.ChunkModelBuilder;
import org.embeddedt.embeddium.impl.render.chunk.data.MinecraftBuiltRenderSectionData;
import org.embeddedt.embeddium.impl.render.chunk.sprite.SpriteTransparencyLevel;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexEncoder;
import org.embeddedt.embeddium.impl.util.QuadUtil;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.system.MemoryUtil;
import org.taumc.celeritas.impl.extensions.SpriteExtension;
import org.taumc.celeritas.impl.extensions.TextureMapExtension;
import org.taumc.celeritas.impl.render.terrain.VintageRenderPassConfigurationBuilder;
import org.taumc.celeritas.impl.world.WorldSlice;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

public class VintageChunkBuildContext extends ChunkBuildContext {
    public static final BlockRenderLayer[] LAYERS = BlockRenderLayer.values();
    private final TextureMapExtension textureAtlas;
    private final BufferBuilder[] worldRenderers = new BufferBuilder[LAYERS.length];
    private final boolean[] usedWorldRenderers = new boolean[LAYERS.length];
    private int offX, offY, offZ;
    @Getter
    private final WorldSlice worldSlice;
    private final RenderPassConfiguration<?> renderPassConfiguration;

    public VintageChunkBuildContext(WorldClient world, RenderPassConfiguration renderPassConfiguration) {
        super(renderPassConfiguration);
        this.renderPassConfiguration = renderPassConfiguration;
        this.worldSlice = new WorldSlice(world);
        this.textureAtlas = (TextureMapExtension) Minecraft.getMinecraft().getTextureMapBlocks();
    }

    public void setupTranslation(int x, int y, int z) {
        this.offX = x;
        this.offY = y;
        this.offZ = z;
    }

    public BufferBuilder getBufferBuilderForLayer(BlockRenderLayer layer) {
        var builder = this.worldRenderers[layer.ordinal()];
        if (builder == null) {
            builder = new BufferBuilder(131072);
            this.worldRenderers[layer.ordinal()] = builder;
        }
        if (!this.usedWorldRenderers[layer.ordinal()]) {
            builder.begin(GL11C.GL_QUADS, DefaultVertexFormats.BLOCK);
            builder.setTranslation(-this.offX, -this.offY, -this.offZ);
            this.usedWorldRenderers[layer.ordinal()] = true;
        }
        return builder;
    }

    public void convertVanillaDataToCeleritasData(ChunkBuildBuffers buffers) {
        var renderers = this.worldRenderers;
        var used = this.usedWorldRenderers;
        for (int i = 0; i < renderers.length; i++) {
            if(!used[i]) {
                continue;
            }
            var bufferBuilder = Objects.requireNonNull(renderers[i]);
            bufferBuilder.finishDrawing();
            ByteBuffer rawBuffer = bufferBuilder.getByteBuffer();
            var material = buffers.getRenderPassConfiguration().getMaterialForRenderType(LAYERS[i]);
            copyBlockData(rawBuffer, buffers, material);
        }
    }

    @Override
    public void cleanup() {
        super.cleanup();
        this.worldSlice.reset();
        Arrays.fill(this.usedWorldRenderers, false);
    }

    private Material selectMaterial(Material material, TextureAtlasSprite sprite) {
        if (sprite != null && sprite.getClass() == TextureAtlasSprite.class && !sprite.hasAnimationMetadata()) {
            var transparencyLevel = ((SpriteExtension)sprite).celeritas$getTransparencyLevel();
            if (transparencyLevel == SpriteTransparencyLevel.OPAQUE) {
                // Downgrade to solid
                return this.renderPassConfiguration.defaultSolidMaterial();
            } else if (material == this.renderPassConfiguration.defaultTranslucentMaterial() && transparencyLevel != SpriteTransparencyLevel.TRANSLUCENT) {
                // Downgrade to cutout
                return this.renderPassConfiguration.defaultCutoutMippedMaterial();
            }
        }
        return material;
    }

    private void copyBlockData(ByteBuffer source, ChunkBuildBuffers buffers, Material material) {
        int vsize = DefaultVertexFormats.BLOCK.getSize();
        int numQuads = source.limit() / (vsize * 4);
        long ptr = MemoryUtil.memAddress(source);
        var quad = ChunkVertexEncoder.Vertex.uninitializedQuad();
        var animatedSpritesList = ((MinecraftBuiltRenderSectionData<TextureAtlasSprite, TileEntity>)buffers.getSectionContextBundle()).animatedSprites;
        for(int q = 0; q < numQuads; q++) {
            float uSum = 0, vSum = 0;
            for(int v = 0; v < 4; v++) {
                var vertex = quad[v];
                vertex.x = MemoryUtil.memGetFloat(ptr);
                vertex.y = MemoryUtil.memGetFloat(ptr + 4);
                vertex.z = MemoryUtil.memGetFloat(ptr + 8);
                vertex.color = MemoryUtil.memGetInt(ptr + 12);
                vertex.u = MemoryUtil.memGetFloat(ptr + 16);
                vertex.v = MemoryUtil.memGetFloat(ptr + 20);
                uSum += vertex.u;
                vSum += vertex.v;
                vertex.light = MemoryUtil.memGetInt(ptr + 24);
                ptr += vsize;
            }
            TextureAtlasSprite sprite = this.textureAtlas.celeritas$findFromUV(uSum * 0.25f, vSum * 0.25f);
            if (sprite != null && sprite.hasAnimationMetadata()) {
                animatedSpritesList.add(sprite);
            }
            int trueNormal = QuadUtil.calculateNormal(quad);
            for (int v = 0; v < 4; v++) {
                var vertex = quad[v];
                vertex.vanillaNormal = trueNormal;
                vertex.trueNormal = trueNormal;
            }
            ModelQuadFacing facing = QuadUtil.findNormalFace(trueNormal);
            Material correctMaterial = selectMaterial(material, sprite);
            buffers.get(correctMaterial).getVertexBuffer(facing).push(quad, material);
        }
    }
}
