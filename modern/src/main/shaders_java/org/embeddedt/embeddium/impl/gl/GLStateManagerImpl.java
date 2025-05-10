package org.embeddedt.embeddium.impl.gl;

import com.mitchej123.glsm.GLStateManagerService;
import com.mojang.blaze3d.platform.GlStateManager;
import net.irisshaders.iris.gl.blending.BlendMode;
import net.irisshaders.iris.gl.blending.ColorMask;
import net.irisshaders.iris.mixin.GlStateManagerAccessor;
import net.irisshaders.iris.mixin.statelisteners.BooleanStateAccessor;

public class GLStateManagerImpl implements GLStateManagerService {
    @Override
    public int glGetInteger(int pname) {
        return GlStateManager._getInteger(pname);
    }

    @Override
    public String glGetString(int pname) {
        return GlStateManager._getString(pname);
    }

    @Override
    public void glBindFramebuffer(int target, int framebuffer) {
        GlStateManager._glBindFramebuffer(target, framebuffer);
    }

    @Override
    public int glCheckFramebufferStatus(int target) {
        return GlStateManager.glCheckFramebufferStatus(target);
    }

    @Override
    public void glDeleteFramebuffers(int framebuffer) {
        GlStateManager._glDeleteFramebuffers(framebuffer);
    }

    @Override
    public int glGenFramebuffers() {
        return GlStateManager.glGenFramebuffers();
    }

    @Override
    public int glGetProgrami(int program, int pname) {
        return GlStateManager.glGetProgrami(program, pname);
    }

    @Override
    public void glAttachShader(int program, int shader) {
        GlStateManager.glAttachShader(program, shader);
    }

    @Override
    public void glDeleteShader(int shader) {
        GlStateManager.glDeleteShader(shader);
    }

    @Override
    public int glCreateShader(int type) {
        return GlStateManager.glCreateShader(type);
    }

    @Override
    public void glCompileShader(int shader) {
        GlStateManager.glCompileShader(shader);
    }

    @Override
    public int glGetShaderi(int shader, int pname) {
        return GlStateManager.glGetShaderi(shader, pname);
    }

    @Override
    public void glUseProgram(int program) {
        GlStateManager._glUseProgram(program);
    }

    @Override
    public int glCreateProgram() {
        return GlStateManager.glCreateProgram();
    }

    @Override
    public void glDeleteProgram(int program) {
        GlStateManager.glDeleteProgram(program);
    }

    @Override
    public void glLinkProgram(int program) {
        GlStateManager.glLinkProgram(program);
    }

    @Override
    public int glGetUniformLocation(int program, CharSequence name) {
        return GlStateManager._glGetUniformLocation(program, name);
    }

    @Override
    public void glUniform1i(int location, int value) {
        GlStateManager._glUniform1i(location, value);
    }

    @Override
    public int glGetAttribLocation(int program, CharSequence name) {
        return GlStateManager._glGetAttribLocation(program, name);
    }

    @Override
    public void glBindAttribLocation(int program, int index, CharSequence name) {
        GlStateManager._glBindAttribLocation(program, index, name);
    }

    @Override
    public int glGenVertexArrays() {
        return GlStateManager._glGenVertexArrays();
    }

    @Override
    public void glBindVertexArray(int array) {
        GlStateManager._glBindVertexArray(array);
    }

    @Override
    public void glCopyTexSubImage2D(int target, int level, int xoffset, int yoffset, int x, int y, int width, int height) {
        GlStateManager._glCopyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height);
    }

    @Override
    public void enableCullFace() {
        GlStateManager._enableCull();
    }

    @Override
    public void disableCullFace() {
        GlStateManager._disableCull();
    }

    @Override
    public void enableBlend() {
        GlStateManager._enableBlend();
    }

    @Override
    public void disableBlend() {
        GlStateManager._disableBlend();
    }

    @Override
    public void glBlendFuncSeparate(int srcRGB, int dstRGB, int srcAlpha, int dstAlpha) {
        GlStateManager._blendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha);
    }

    @Override
    public void enableDepthTest() {
        GlStateManager._enableDepthTest();
    }

    @Override
    public void disableDepthTest() {
        GlStateManager._disableDepthTest();
    }

    @Override
    public void glDepthFunc(int func) {

    }

    @Override
    public void glDepthMask(boolean flag) {
        GlStateManager._depthMask(flag);
    }

    @Override
    public void glViewport(int x, int y, int width, int height) {
        GlStateManager._viewport(x, y, width, height);
    }

    @Override
    public void glColorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        GlStateManager._colorMask(red, green, blue, alpha);
    }

    @Override
    public void glClearColor(float red, float green, float blue, float alpha) {
        GlStateManager._clearColor(red, green, blue, alpha);
    }

    @Override
    public void clear(int mask, boolean checkError) {
        GlStateManager._clear(mask, checkError);
    }

    @Override
    public int glGetTexLevelParameteri(int target, int level, int pname) {
        return GlStateManager._getTexLevelParameter(target, level, pname);
    }

    @Override
    public void glFramebufferTexture2D(int target, int attachment, int textarget, int texture, int level) {
        GlStateManager._glFramebufferTexture2D(target, attachment, textarget, texture, level);
    }

    @Override
    public int glGenTextures() {
        return GlStateManager._genTexture();
    }

    @Override
    public void glGenTextures(int[] textures) {
        GlStateManager._genTextures(textures);
    }

    @Override
    public void glActiveTexture(int texture) {
        GlStateManager._activeTexture(texture);
    }

    @Override
    public int getActiveTexture() {
        return GlStateManager._getActiveTexture();
    }

    @Override
    public int getActiveTextureAccessor() {
        return GlStateManagerAccessor.getActiveTexture();
    }

    @Override
    public int getBoundTexture(int internalUnit) {
        return GlStateManagerAccessor.getTEXTURES()[internalUnit].binding;
    }

    @Override
    public int getActiveBoundTexture() {
        return GlStateManagerAccessor.getTEXTURES()[GlStateManagerAccessor.getActiveTexture()].binding;
    }

    @Override
    public int getViewportWidth() {
        return GlStateManager.Viewport.width();
    }

    @Override
    public int getViewportHeight() {
        return GlStateManager.Viewport.height();
    }

    @Override
    public void setBoundTexture(int unit, int texture) {
        GlStateManagerAccessor.getTEXTURES()[unit].binding = texture;
    }

    @Override
    public void glDeleteTextures(int texture) {
        GlStateManager._deleteTexture(texture);
    }

    @Override
    public void glDeleteTextures(int[] textures) {
        GlStateManager._deleteTextures(textures);
    }

    @Override
    public void bindTexture(int texture) {
        GlStateManager._bindTexture(texture);
    }

    @Override
    public int glGenBuffers() {
        return GlStateManager._glGenBuffers();
    }

    @Override
    public void glBindBuffer(int target, int buffer) {
        GlStateManager._glBindBuffer(target, buffer);
    }

    @Override
    public void glPixelStorei(int pname, int param) {
        GlStateManager._pixelStore(pname, param);
    }

    @Override
    public boolean getDepthStateMask() {
        return GlStateManagerAccessor.getDEPTH().mask;
    }

    @Override
    public boolean isBlendEnabled() {
        GlStateManager.BlendState blendState = GlStateManagerAccessor.getBLEND();

        return ((BooleanStateAccessor)blendState.mode).isEnabled();
    }

    @Override
    public BlendMode getBlendMode() {
        GlStateManager.BlendState blendState = GlStateManagerAccessor.getBLEND();
        return new BlendMode(blendState.srcRgb, blendState.dstRgb, blendState.srcAlpha, blendState.dstAlpha);
    }

    @Override
    public ColorMask getColorMask() {
        GlStateManager.ColorMask colorMask = GlStateManagerAccessor.getCOLOR_MASK();

        return new ColorMask(colorMask.red, colorMask.green, colorMask.blue, colorMask.alpha);
    }

}
