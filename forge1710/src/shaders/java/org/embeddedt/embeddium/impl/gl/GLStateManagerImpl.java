package org.embeddedt.embeddium.impl.gl;

import com.mitchej123.glsm.GLStateManagerService;
import net.irisshaders.iris.gl.blending.BlendMode;
import net.irisshaders.iris.gl.blending.ColorMask;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.opengl.*;

public class GLStateManagerImpl implements GLStateManagerService {
    @Override
    public int glGetInteger(int pname) {
        return GL11.glGetInteger(pname);
    }

    @Override
    public String glGetString(int pname) {
        return GL11.glGetString(pname);
    }

    @Override
    public void glBindFramebuffer(int target, int framebuffer) {
        OpenGlHelper.func_153171_g/*glBindFramebuffer*/(target, framebuffer);
    }

    @Override
    public int glCheckFramebufferStatus(int target) {
        return OpenGlHelper.func_153167_i/*glCheckFramebufferStatus*/(target);
    }

    @Override
    public void glDeleteFramebuffers(int framebuffer) {
        OpenGlHelper.func_153174_h/*glDeleteFramebuffers*/(framebuffer);
    }

    @Override
    public int glGenFramebuffers() {
        return OpenGlHelper.func_153165_e/*glGenFramebuffers*/();
    }

    @Override
    public int glGetProgrami(int program, int pname) {
        return OpenGlHelper.glGetProgrami(program, pname);
    }

    @Override
    public void glAttachShader(int program, int shader) {
        OpenGlHelper.glAttachShader(program, shader);
    }

    @Override
    public void glDeleteShader(int shader) {
        OpenGlHelper.glDeleteShader(shader);
    }

    @Override
    public int glCreateShader(int type) {
        return OpenGlHelper.glCreateShader(type);
    }

    @Override
    public void glCompileShader(int shader) {
        OpenGlHelper.glCompileShader(shader);
    }

    @Override
    public int glGetShaderi(int shader, int pname) {
        return OpenGlHelper.glGetShaderi(shader, pname);
    }

    @Override
    public void glUseProgram(int program) {
        OpenGlHelper.glUseProgram(program);
    }

    @Override
    public int glCreateProgram() {
        return OpenGlHelper.glCreateProgram();
    }

    @Override
    public void glDeleteProgram(int program) {
        OpenGlHelper.glDeleteProgram(program);
    }

    @Override
    public void glLinkProgram(int program) {
        OpenGlHelper.glLinkProgram(program);
    }

    @Override
    public int glGetUniformLocation(int program, CharSequence name) {
        return OpenGlHelper.glGetUniformLocation(program, name);
    }

    @Override
    public void glUniform1i(int location, int value) {
        OpenGlHelper.glUniform1i(location, value);
    }

    @Override
    public int glGetAttribLocation(int program, CharSequence name) {
        return OpenGlHelper.glGetAttribLocation(program, name);
    }

    @Override
    public void glBindAttribLocation(int program, int index, CharSequence name) {
        GL20.glBindAttribLocation(program, index, name);
    }

    @Override
    public int glGenVertexArrays() {
        return GL30.glGenVertexArrays();
    }

    @Override
    public void glBindVertexArray(int array) {
        GL30.glBindVertexArray(array);
    }

    @Override
    public void glCopyTexSubImage2D(int target, int level, int xoffset, int yoffset, int x, int y, int width, int height) {
        GL11.glCopyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height);
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
    public void glBlendFuncSeparate(int srcRGB, int dstRGB, int srcAlpha, int dstAlpha) {
        OpenGlHelper.glBlendFunc(srcRGB, dstRGB, srcAlpha, dstAlpha);
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
    public void glDepthFunc(int func) {
        GL11.glDepthFunc(func);
    }

    @Override
    public void glDepthMask(boolean flag) {
        GL11.glDepthMask(flag);
    }

    @Override
    public void glViewport(int x, int y, int width, int height) {
        GL11.glViewport(x, y, width, height);
    }

    @Override
    public void glColorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        GL11.glColorMask(red, green, blue, alpha);
    }

    @Override
    public void glClearColor(float red, float green, float blue, float alpha) {
        GL11.glClearColor(red, green, blue, alpha);
    }

    @Override
    public int glGetTexLevelParameteri(int target, int level, int pname) {
        return GL11.glGetTexLevelParameteri(target, level, pname);
    }

    @Override
    public void glFramebufferTexture2D(int target, int attachment, int textarget, int texture, int level) {
        OpenGlHelper.func_153188_a/*glFramebufferTexture2D*/(target, attachment, textarget, texture, level);
    }

    @Override
    public int glGenTextures() {
        return GL11.glGenTextures();
    }

    @Override
    public void glGenTextures(int[] textures) {
        GL11.glGenTextures(textures);
    }

    @Override
    public void glDeleteTextures(int texture) {
        GL11.glDeleteTextures(texture);
    }

    @Override
    public void glDeleteTextures(int[] textures) {
        GL11.glDeleteTextures(textures);
    }

    @Override
    public void glActiveTexture(int texture) {
        GL13.glActiveTexture(texture);
    }

    @Override
    public int glGenBuffers() {
        return GL15.glGenBuffers();
    }

    @Override
    public void glBindBuffer(int target, int buffer) {
        GL15.glBindBuffer(target, buffer);
    }

    @Override
    public void glPixelStorei(int pname, int param) {
        GL11.glPixelStorei(pname, param);
    }

    @Override
    public void clear(int mask, boolean checkError) {
        GL11.glClear(mask);
    }

    @Override
    public void bindTexture(int texture) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
    }

    @Override
    public int getActiveTexture() {
        return GL13.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
    }

    @Override
    public int getActiveTextureAccessor() {
        return GL13.GL_ACTIVE_TEXTURE;
    }

    @Override
    public int getBoundTexture(int internalUnit) {
        return GL11.glGetInteger(internalUnit);
    }

    @Override
    public int getActiveBoundTexture() {
        return GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
    }

    @Override
    public int getViewportWidth() {
        return 0;
    }

    @Override
    public int getViewportHeight() {
        return 0;
    }

    @Override
    public boolean getDepthStateMask() {
        return GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
    }

    @Override
    public boolean isBlendEnabled() {
        return GL11.glGetBoolean(GL11.GL_BLEND);
    }

    @Override
    public BlendMode getBlendMode() {
        return new BlendMode(GL11.glGetInteger(GL11.GL_SRC_COLOR), GL11.glGetInteger(GL11.GL_DST_COLOR), GL11.glGetInteger(GL11.GL_SRC_ALPHA), GL11.glGetInteger(GL11.GL_DST_ALPHA));
    }

    @Override
    public ColorMask getColorMask() {
        return new ColorMask(GL11.glGetBoolean(GL11.GL_RED_BITS), GL11.glGetBoolean(GL11.GL_GREEN_BITS), GL11.glGetBoolean(GL11.GL_BLUE_BITS), GL11.glGetBoolean(GL11.GL_ALPHA_BITS));
    }

    @Override
    public void setBoundTexture(int unit, int texture) {
        GL11.glBindTexture(GL13.GL_TEXTURE0 + unit, texture);
    }
}
