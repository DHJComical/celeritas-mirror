package org.taumc.celeritas.impl.render.terrain.compile;

import net.minecraft.world.World;
import org.embeddedt.embeddium.impl.common.util.MathUtil;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.embeddedt.embeddium.impl.render.chunk.RenderPassConfiguration;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildBuffers;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildContext;
import org.embeddedt.embeddium.impl.render.chunk.sprite.SpriteTransparencyLevel;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexEncoder;
import org.embeddedt.embeddium.impl.util.QuadUtil;
import org.taumc.celeritas.impl.render.terrain.CeleritasWorldRenderer;
import org.taumc.celeritas.impl.render.terrain.PrimitiveRenderPassConfigurationBuilder;
import org.taumc.celeritas.impl.render.terrain.SpriteTransparencyTracker;

public class PrimitiveChunkBuildContext extends ChunkBuildContext {
    public static final int NUM_PASSES = 2;

    public final World world;
    private final SpriteTransparencyTracker transparencyTracker;

    public PrimitiveChunkBuildContext(World world, RenderPassConfiguration renderPassConfiguration) {
        super(renderPassConfiguration);
        this.world = world;
        this.transparencyTracker = CeleritasWorldRenderer.instance().getTransparencyTracker();
    }

    private final ChunkVertexEncoder.Vertex[] vertices = ChunkVertexEncoder.Vertex.uninitializedQuad();

    private Material selectMaterial(Material material, SpriteTransparencyLevel transparencyLevel) {
        if (transparencyLevel == SpriteTransparencyLevel.OPAQUE && material == PrimitiveRenderPassConfigurationBuilder.CUTOUT_MIPPED_MATERIAL) {
            // Downgrade to solid
            return PrimitiveRenderPassConfigurationBuilder.SOLID_MATERIAL;
        } else if (material == PrimitiveRenderPassConfigurationBuilder.TRANSLUCENT_MATERIAL && transparencyLevel != SpriteTransparencyLevel.TRANSLUCENT) {
            // Downgrade to cutout
            return PrimitiveRenderPassConfigurationBuilder.CUTOUT_MIPPED_MATERIAL;
        }
        return material;
    }

    private static final int[] NORMAL_WINDING = new int[] {0, 1, 2, 3};
    private static final int[] BACKFACE_WINDING = new int[] {3, 2, 1, 0};

    public void copyRawBuffer(int[] rawBuffer, int vertexCount, ChunkBuildBuffers buffers, Material material) {
        if (vertexCount == 0) {
            return;
        }

        // Require
        if ((vertexCount & 0x3) != 0) {
            throw new IllegalStateException();
        }

        var celeritasVertices = this.vertices;

        int numQuads = vertexCount / 4;
        outputQuads(rawBuffer, celeritasVertices, numQuads, buffers, material, NORMAL_WINDING);
        //? if <1.7 {
        if (material.pass == PrimitiveRenderPassConfigurationBuilder.TRANSLUCENT_PASS) {
            // We need to emulate backface culling as the legacy renderer relies on it. Write the same quads again
            // in reverse winding order.
            outputQuads(rawBuffer, celeritasVertices, numQuads, buffers, material, BACKFACE_WINDING);
        }
        //?}
    }

    private void outputQuads(int[] rawBuffer, ChunkVertexEncoder.Vertex[] celeritasVertices, int numQuads, ChunkBuildBuffers buffers, Material material, int[] winding) {
        int ptr = 0;
        var tracker = this.transparencyTracker;
        for (int quadIdx = 0; quadIdx < numQuads; quadIdx++) {
            float uSum = 0, vSum = 0;
            for (int vIdx = 0; vIdx < 4; vIdx++) {
                var vertex = celeritasVertices[winding[vIdx]];
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
                celeritasVertices[winding[vIdx]].trueNormal = trueNormal;
            }
            ModelQuadFacing facing = QuadUtil.findNormalFace(trueNormal);
            // TODO implement render pass downgrading for 1.5+
            //? if <1.5 {
            // Pretend the atlas is 256x256 for the purposes of computing the sprite ID
            uSum /= 4;
            vSum /= 4;
            int spriteX = MathUtil.mojfloor(uSum * 256.0f);
            int spriteY = MathUtil.mojfloor(vSum * 256.0f);
            int spriteId = (spriteY / 16) * 16 + spriteX / 16;
            Material correctMaterial = selectMaterial(material, tracker.getTransparencyLevel(spriteId));
            //?} else
            /*Material correctMaterial = material;*/
            buffers.get(correctMaterial).getVertexBuffer(facing).push(celeritasVertices, correctMaterial);
        }
    }

    @Override
    public void cleanup() {
        super.cleanup();
    }
}
