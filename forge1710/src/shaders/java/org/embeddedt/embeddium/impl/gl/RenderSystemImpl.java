package org.embeddedt.embeddium.impl.gl;

import com.mitchej123.glsm.RenderSystemService;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import net.minecraft.client.renderer.OpenGlHelper;

import java.nio.FloatBuffer;

public class RenderSystemImpl implements RenderSystemService {
    @Override
    public void glActiveTexture(int texture) {
        GL13.glActiveTexture(texture);
    }

    @Override
    public void enableCullFace() {
        GL11.glEnable(GL11.GL_CULL_FACE);
    }

    @Override
    public void disableCullFace() {
        GL11.glDisable(GL11.GL_CULL_FACE);
    }

    @Override
    public void enableBlend() {
        GL11.glEnable(GL11.GL_BLEND);
    }

    @Override
    public void disableBlend() {
        GL11.glDisable(GL11.GL_BLEND);
    }

    @Override
    public void setUnknownBlendState() {

    }

    @Override
    public void enableDepthTest() {
        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    @Override
    public void disableDepthTest() {
        GL11.glDisable(GL11.GL_DEPTH_TEST);
    }

    @Override
    public void depthFunc(int depthFunc) {
        GL11.glDepthFunc(depthFunc);
    }

    @Override
    public void depthMask(boolean flag) {
        GL11.glDepthMask(flag);
    }

    @Override
    public void glViewport(int x, int y, int width, int height) {
        GL11.glViewport(x, y, width, height);
    }

    @Override
    public void bindTexture(int texture) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
    }

    @Override
    public void glUniform1i(int location, int value) {
        OpenGlHelper.glUniform1i(location, value);
    }

    @Override
    public void glUniformMatrix3(int location, boolean transpose, FloatBuffer value) {
        OpenGlHelper.glUniformMatrix3(location, transpose, value);
    }

    @Override
    public void glUniformMatrix4(int location, boolean transpose, FloatBuffer value) {
        OpenGlHelper.glUniformMatrix4(location, transpose, value);
    }

    @Override
    public void glClearColor(float red, float green, float blue, float alpha) {
        GL11.glClearColor(red, green, blue, alpha);
    }

    @Override
    public void clear(int mask, boolean checkError) {
        GL11.glClear(mask);
        if (checkError) {
            int error = GL11.glGetError();
            if (error != 0) {
                throw new RuntimeException("OpenGL Error: " + error);
            }
        }
    }

    @Override
    public void assertOnRenderThread() {

    }

    @Override
    public void assertOnRenderThreadOrInit() {

    }

    @Override
    public void setShaderTexture(int shaderTexture, int textureId) {
        // TODO
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public int getShaderTexture(int shaderTexture) {
        // TODO
        throw new UnsupportedOperationException("Not implemented yet");
//        return 0;
    }

    @Override
    public void setShaderColor(float red, float green, float blue, float alpha) {
        // TODO
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public float[] getShaderFogColor() {
        return new float[0];
    }

    @Override
    public float getShaderFogStart() {
        return 0;
    }

    @Override
    public float getShaderFogEnd() {
        return 0;
    }

    @Override
    public int getFogShape() {
        return 0;
    }

    @Override
    public float getShaderLineWidth() {
        return 0;
    }

    private final FloatBuffer PROJECTION_MATRIX_BUFFER = org.lwjgl.BufferUtils.createFloatBuffer(16);
    private final Matrix4f PROJECTION_MATRIX = new Matrix4f();
    @Override
    public Matrix4f getProjectionMatrix() {

        GL11.glGetFloatv(GL11.GL_PROJECTION_MATRIX, PROJECTION_MATRIX_BUFFER);
        return PROJECTION_MATRIX.set(PROJECTION_MATRIX_BUFFER);
    }

    @Override
    public void setProjectionMatrixOrth(Matrix4f projectionMatrix) {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        projectionMatrix.get(0, PROJECTION_MATRIX_BUFFER);
        GL11.glLoadMatrixf(PROJECTION_MATRIX_BUFFER);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }

    @Override
    public void setProjectionMatrixOrigin(Matrix4f projectionMatrix) {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        projectionMatrix.get(0, PROJECTION_MATRIX_BUFFER);
        GL11.glLoadMatrixf(PROJECTION_MATRIX_BUFFER);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }
}
