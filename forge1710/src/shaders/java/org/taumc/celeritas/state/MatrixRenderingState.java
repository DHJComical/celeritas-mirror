package org.taumc.celeritas.state;

import lombok.Getter;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

public class MatrixRenderingState {
    public static final MatrixRenderingState INSTANCE = new MatrixRenderingState();

    @Getter private final FloatBuffer projectionBuffer = BufferUtils.createFloatBuffer(16);
    @Getter private final FloatBuffer modelViewBuffer = BufferUtils.createFloatBuffer(16);
    @Getter private final Matrix4f projectionMatrix = new Matrix4f().identity();
    @Getter private final Matrix4f modelViewMatrix = new Matrix4f().identity();


    public void setProjectionMatrix(FloatBuffer projection) {
        projectionMatrix.set(projection);
        projectionMatrix.get(0, projectionBuffer);

    }

    public void setModelViewMatrix(FloatBuffer modelview) {
        modelViewMatrix.set(modelview);
        modelViewMatrix.get(0, modelViewBuffer);
    }

}
