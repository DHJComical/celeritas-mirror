package org.taumc.celeritas.impl.render.terrain.matrix;

import net.minecraft.client.util.GlAllocationUtils;
import org.embeddedt.embeddium.impl.render.chunk.ChunkRenderMatrices;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;

public class PrimitiveChunkMatrixGetter {
    private static final FloatBuffer PROJECTION = GlAllocationUtils.allocateFloatBuffer(16);
    private static final FloatBuffer MODELVIEW = GlAllocationUtils.allocateFloatBuffer(16);

    public static ChunkRenderMatrices getMatrices() {
        GL11.glGetFloatv(GL11.GL_PROJECTION_MATRIX, PROJECTION);
        GL11.glGetFloatv(GL11.GL_MODELVIEW_MATRIX, MODELVIEW);
        return new ChunkRenderMatrices(
                new Matrix4f(PROJECTION),
                new Matrix4f(MODELVIEW)
        );
    }
}
