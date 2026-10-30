package org.embeddedt.embeddium.impl.gl.texture;

import org.embeddedt.embeddium.impl.gl.GlObject;
import org.embeddedt.embeddium.impl.gl.buffer.GlBuffer;
import org.embeddedt.embeddium.impl.gl.functions.TextureBufferFunctions;
import org.taumc.celeritas.lwjgl.GL30;
import org.taumc.celeritas.lwjgl.GL31;

import static org.taumc.celeritas.lwjgl.LWJGLServiceProvider.LWJGL;

/**
 * A texture buffer exposing a buffer object to shaders as an array of 32-bit unsigned integers.
 */
public class GlBufferTexture extends GlObject {
    private GlBuffer attachedBuffer;

    public GlBufferTexture() {
        this.setHandle(LWJGL.glGenTextures());
    }

    /**
     * Binds this texture to the active texture unit, attaching the given buffer if it is not already attached. Buffers
     * are compared by identity, because a deleted buffer's name may be reused by the buffer that replaces it.
     */
    public void bind(TextureBufferFunctions functions, GlBuffer buffer) {
        LWJGL.glBindTexture(GL31.GL_TEXTURE_BUFFER, this.handle());

        if (this.attachedBuffer != buffer) {
            functions.texBuffer(GL31.GL_TEXTURE_BUFFER, GL30.GL_R32UI, buffer);
            this.attachedBuffer = buffer;
        }
    }

    @Override
    protected void destroyInternal() {
        LWJGL.glDeleteTextures(this.handle());
        this.attachedBuffer = null;
    }
}
