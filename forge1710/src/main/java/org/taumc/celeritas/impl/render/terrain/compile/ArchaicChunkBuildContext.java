package org.taumc.celeritas.impl.render.terrain.compile;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.embeddedt.embeddium.impl.render.chunk.RenderPassConfiguration;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildBuffers;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildContext;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexEncoder;
import org.embeddedt.embeddium.impl.util.QuadUtil;
import org.taumc.celeritas.impl.extensions.TextureMapExtension;

public class ArchaicChunkBuildContext extends ChunkBuildContext {
    public static final int NUM_PASSES = 2;

    private final TextureMapExtension textureAtlas;

    public ArchaicChunkBuildContext(WorldClient world, RenderPassConfiguration renderPassConfiguration) {
        super(renderPassConfiguration);
        this.textureAtlas = (TextureMapExtension)Minecraft.getMinecraft().getTextureMapBlocks();
    }

    private final ChunkVertexEncoder.Vertex[] vertices = ChunkVertexEncoder.Vertex.uninitializedQuad();

    public void copyRawBuffer(int[] rawBuffer, int vertexCount, ChunkBuildBuffers buffers, Material material) {
        if (vertexCount == 0) {
            return;
        }

        var holder = buffers.get(material);
        var animatedSpritesList = holder.getSectionContextBundle().getContext(ArchaicRenderSectionBuiltInfo.ANIMATED_SPRITES);

        // Require
        if ((vertexCount & 0x3) != 0) {
            throw new IllegalStateException();
        }

        var celeritasVertices = this.vertices;

        int ptr = 0;
        int numQuads = vertexCount / 4;
        for (int quadIdx = 0; quadIdx < numQuads; quadIdx++) {
            float uSum = 0, vSum = 0;
            for (int vIdx = 0; vIdx < 4; vIdx++) {
                var vertex = celeritasVertices[vIdx];
                vertex.x = Float.intBitsToFloat(rawBuffer[ptr++]);
                vertex.y = Float.intBitsToFloat(rawBuffer[ptr++]);
                vertex.z = Float.intBitsToFloat(rawBuffer[ptr++]);
                float u = Float.intBitsToFloat(rawBuffer[ptr++]);
                float v = Float.intBitsToFloat(rawBuffer[ptr++]);
                vertex.u = u;
                uSum += u;
                vertex.v = v;
                vSum += v;
                vertex.color = rawBuffer[ptr++];
                vertex.vanillaNormal = rawBuffer[ptr++];
                vertex.light = rawBuffer[ptr++];
            }
            int trueNormal = QuadUtil.calculateNormal(celeritasVertices);
            for (int vIdx = 0; vIdx < 4; vIdx++) {
                celeritasVertices[vIdx].trueNormal = trueNormal;
            }
            ModelQuadFacing facing = QuadUtil.findNormalFace(trueNormal);
            TextureAtlasSprite sprite = this.textureAtlas.celeritas$findFromUV(uSum * 0.25f, vSum * 0.25f);
            if (sprite != null && sprite.hasAnimationMetadata()) {
                animatedSpritesList.add(sprite);
            }
            holder.getVertexBuffer(facing).push(celeritasVertices, material);
        }
    }

    @Override
    public void cleanup() {
        super.cleanup();
    }

    /*
    private void copyBlockData(ByteBuffer source, ChunkModelBuilder dest, Material material) {
        int vsize = DefaultVertexFormats.BLOCK.getSize();
        int numQuads = source.limit() / (vsize * 4);
        long ptr = MemoryUtil.memAddress(source);
        var quad = ChunkVertexEncoder.Vertex.uninitializedQuad();
        for(int q = 0; q < numQuads; q++) {
            for(int v = 0; v < 4; v++) {
                var vertex = quad[v];
                vertex.x = MemoryUtil.memGetFloat(ptr);
                vertex.y = MemoryUtil.memGetFloat(ptr + 4);
                vertex.z = MemoryUtil.memGetFloat(ptr + 8);
                vertex.color = MemoryUtil.memGetInt(ptr + 12);
                vertex.u = MemoryUtil.memGetFloat(ptr + 16);
                vertex.v = MemoryUtil.memGetFloat(ptr + 20);
                vertex.light = MemoryUtil.memGetInt(ptr + 24);
                ptr += vsize;
            }
            dest.getVertexBuffer(ModelQuadFacing.UNASSIGNED).push(quad, material);
        }
    }

     */
}
