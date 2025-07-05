package com.mitchej123.glsm;

import net.irisshaders.iris.gl.blending.BlendMode;
import net.irisshaders.iris.gl.blending.ColorMask;

import java.util.ServiceLoader;

public interface GLStateManagerService {
    GLStateManagerService GL_STATE_MANAGER = ServiceLoader.load(GLStateManagerService.class).findFirst().orElseThrow();

    int glGetInteger(int pname);
    String glGetString(int pname);

    void glBindFramebuffer(int target, int framebuffer);
    int glCheckFramebufferStatus(int target);
    void glDeleteFramebuffers(int framebuffer);

    int glGenFramebuffers();

    int glGetProgrami(int program, int pname);
    void glAttachShader(int program, int shader);
    void glDeleteShader(int shader);
    int glCreateShader(int type);
    void glCompileShader(int shader);
    int glGetShaderi(int shader, int pname);
    void glUseProgram(int program);
    int glCreateProgram();
    void glDeleteProgram(int program);
    void glLinkProgram(int program);
    int glGetUniformLocation(int program, CharSequence name);
    void glUniform1i(int location, int value);

    int glGetAttribLocation(int program, CharSequence name);
    void glBindAttribLocation(int program, int index, CharSequence name);

    int glGenVertexArrays();
    void glBindVertexArray(int array);
    void glCopyTexSubImage2D(int target, int level, int xoffset, int yoffset, int x, int y, int width, int height);

    void enableCullFace();
    void disableCullFace();

    void enableBlend();
    void disableBlend();
    void glBlendFuncSeparate(int srcRGB, int dstRGB, int srcAlpha, int dstAlpha);

    void enableDepthTest();
    void disableDepthTest();
    void glDepthFunc(int func);
    void glDepthMask(boolean flag);

    void glViewport(int x, int y, int width, int height);

    void glColorMask(boolean red, boolean green, boolean blue, boolean alpha);

    void glClearColor(float red, float green, float blue, float alpha);

    int glGetTexLevelParameteri(int target, int level, int pname);

    void glFramebufferTexture2D(int target, int attachment, int textarget, int texture, int level);

    int glGenTextures();
    void glGenTextures(int[] textures);
    void glDeleteTextures(int texture);
    void glDeleteTextures(int[] textures);
    void glActiveTexture(int texture);

    int glGenBuffers();
    void glBindBuffer(int target, int buffer);

    void glPixelStorei(int pname, int param);

    // Mojang & Non-Standard
    void clear(int mask, boolean checkError); // Non-standard signature

    void bindTexture(int texture); // Non-standard signature

    int getActiveTexture(); // Mojang Addition
    int getActiveTextureAccessor(); // Mojang Addition
    int getBoundTexture(int internalUnit); // Mojang Addition
    int getActiveBoundTexture(); // Mojang Addition

    int getViewportWidth(); // Mojang Addition
    int getViewportHeight(); // Mojang Addition

    boolean getDepthStateMask();
    boolean isBlendEnabled();
    BlendMode getBlendMode();
    ColorMask getColorMask();

    void setBoundTexture(int unit, int texture);
}
