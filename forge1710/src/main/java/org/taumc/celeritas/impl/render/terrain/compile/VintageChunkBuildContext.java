package org.taumc.celeritas.impl.render.terrain.compile;

import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.Tessellator;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.embeddedt.embeddium.impl.render.chunk.RenderPassConfiguration;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildBuffers;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildContext;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexEncoder;

import java.util.Arrays;

public class VintageChunkBuildContext extends ChunkBuildContext {
    public static final int NUM_PASSES = 2;

    public VintageChunkBuildContext(WorldClient world, RenderPassConfiguration renderPassConfiguration) {
        super(renderPassConfiguration);
    }

    private final ChunkVertexEncoder.Vertex[] vertices = ChunkVertexEncoder.Vertex.uninitializedQuad();

    public void copyRawBuffer(int[] rawBuffer, int vertexCount, ChunkBuildBuffers buffers, Material material) {
        var buffer = buffers.get(material).getVertexBuffer(ModelQuadFacing.UNASSIGNED);

        // Require
        if ((vertexCount & 0x3) != 0) {
            throw new IllegalStateException();
        }

        var celeritasVertices = this.vertices;

        int ptr = 0;
        int numQuads = vertexCount / 4;
        for (int quadIdx = 0; quadIdx < numQuads; quadIdx++) {
            for (int vIdx = 0; vIdx < 4; vIdx++) {
                var vertex = celeritasVertices[vIdx];
                vertex.x = Float.intBitsToFloat(rawBuffer[ptr++]);
                vertex.y = Float.intBitsToFloat(rawBuffer[ptr++]);
                vertex.z = Float.intBitsToFloat(rawBuffer[ptr++]);
                vertex.u = Float.intBitsToFloat(rawBuffer[ptr++]);
                vertex.v = Float.intBitsToFloat(rawBuffer[ptr++]);
                vertex.color = rawBuffer[ptr++];
                vertex.vanillaNormal = rawBuffer[ptr++];
                vertex.light = rawBuffer[ptr++];
                vertex.trueNormal = vertex.vanillaNormal;
            }
            buffer.push(celeritasVertices, material);
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
