package org.embeddedt.embeddium.impl.render.chunk;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import java.nio.FloatBuffer;

public record ChunkRenderMatrices(Matrix4fc projection, Matrix4fc modelView) {
    public ChunkRenderMatrices(FloatBuffer projection, FloatBuffer modelView) {
        this(new Matrix4f(projection), new Matrix4f(modelView));
    }
}
