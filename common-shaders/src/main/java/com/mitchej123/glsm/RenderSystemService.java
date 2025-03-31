package com.mitchej123.glsm;

import java.nio.FloatBuffer;
import java.util.ServiceLoader;

import org.joml.Matrix4f;

public interface RenderSystemService {
    RenderSystemService RENDER_SYSTEM = ServiceLoader.load(RenderSystemService.class).findFirst().orElseThrow();

    void glActiveTexture(int texture);

    void enableCullFace();
    void disableCullFace();

    void enableBlend();
    void disableBlend();
    void setUnknownBlendState(); // Mojang Addition

    void enableDepthTest();
    void disableDepthTest();
    void depthFunc(int depthFunc);
    void depthMask(boolean flag);

    void glViewport(int x, int y, int width, int height);

    void bindTexture(int texture); // Non-standard

    void glUniform1i(int location, int value);
    void glUniformMatrix3(int location, boolean transpose, FloatBuffer value);
    void glUniformMatrix4(int location, boolean transpose, FloatBuffer value);

    void glClearColor(float red, float green, float blue, float alpha);

    // Mojang Implementations
    void clear(int mask, boolean checkError); // Non-standard

    void assertOnRenderThread();
    void assertOnRenderThreadOrInit();

    // TODO: Figure out ResourceLocation, etc
    // void setShaderTexture(int shaderTexture, ResourceLocation textureId)
    void setShaderTexture(int shaderTexture, int textureId);
    int getShaderTexture(int shaderTexture);

    void setShaderColor(float red, float green, float blue, float alpha);

    float[] getShaderFogColor();
    float getShaderFogStart();
    float getShaderFogEnd();
    int getFogShape();

    float getShaderLineWidth();

    Matrix4f getProjectionMatrix();
    void setProjectionMatrixOrth(Matrix4f projectionMatrix);
    void setProjectionMatrixOrigin(Matrix4f projectionMatrix);
    // TODO: Figure out VertexSorting type
    //  void setProjectionMatrix(Matrix4f projectionMatrix, VertexSorting vertexSorting)

    // TODO: Figure out FogShape
    //  FogShape getShaderFogShape()
}
