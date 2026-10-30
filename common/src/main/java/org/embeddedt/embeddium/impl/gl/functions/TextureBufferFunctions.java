package org.embeddedt.embeddium.impl.gl.functions;

import org.embeddedt.embeddium.impl.gl.buffer.GlBuffer;
import org.embeddedt.embeddium.impl.gl.device.RenderDevice;
import org.taumc.celeritas.lwjgl.GL31;
import org.taumc.celeritas.lwjgl.GLExtension;

import static org.taumc.celeritas.lwjgl.LWJGLServiceProvider.LWJGL;

public enum TextureBufferFunctions {
    CORE {
        @Override
        public void texBuffer(int target, int internalFormat, GlBuffer buffer) {
            LWJGL.glTexBuffer(target, internalFormat, buffer.handle());
        }
    },
    ARB {
        @Override
        public void texBuffer(int target, int internalFormat, GlBuffer buffer) {
            LWJGL.glTexBufferARB(target, internalFormat, buffer.handle());
        }
    },
    EXT {
        @Override
        public void texBuffer(int target, int internalFormat, GlBuffer buffer) {
            LWJGL.glTexBufferEXT(target, internalFormat, buffer.handle());
        }
    },
    NONE {
        @Override
        public void texBuffer(int target, int internalFormat, GlBuffer buffer) {
            throw new UnsupportedOperationException("Texture buffers are not supported");
        }
    };

    public abstract void texBuffer(int target, int internalFormat, GlBuffer buffer);

    public boolean isSupported() {
        return this != NONE;
    }

    /**
     * {@return the number of texels a texture buffer may address, or 0 if texture buffers are unsupported}
     */
    int queryMaxTexels() {
        return this.isSupported() ? LWJGL.glGetInteger(GL31.GL_MAX_TEXTURE_BUFFER_SIZE) : 0;
    }

    public static TextureBufferFunctions pickBest(RenderDevice device) {
        // Below GL 3.2 the terrain shaders are GLSL 1.20, which can only sample texture buffers through EXT_gpu_shader4
        if (!LWJGL.isOpenGLVersionSupported(3, 2) && !LWJGL.isExtensionSupported(GLExtension.EXT_gpu_shader4)) {
            return NONE;
        }

        if (LWJGL.isOpenGLVersionSupported(3, 1)) {
            return CORE;
        } else if (LWJGL.isExtensionSupported(GLExtension.ARB_texture_buffer_object)) {
            return ARB;
        } else if (LWJGL.isExtensionSupported(GLExtension.EXT_texture_buffer_object)) {
            return EXT;
        } else {
            return NONE;
        }
    }
}
