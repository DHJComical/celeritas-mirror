package org.embeddedt.embeddium.impl.render.chunk.fog;

import org.embeddedt.embeddium.impl.gl.state.GlStateService;
import org.embeddedt.embeddium.impl.render.ShaderModBridge;
import org.embeddedt.embeddium.impl.render.chunk.DefaultChunkRenderer;
import org.taumc.celeritas.lwjgl.GL11;
import org.taumc.celeritas.lwjgl.GL13;

import static org.taumc.celeritas.lwjgl.LWJGLServiceProvider.LWJGL;

public final class SkyTextureCopy {
    private final GlStateService glState;

    private int texture, width, height;
    private boolean valid;

    public SkyTextureCopy(GlStateService glState) {
        this.glState = glState;
    }

    public static boolean shouldProvide(boolean optionEnabled) {
        return optionEnabled && !ShaderModBridge.areShadersEnabled();
    }

    public int getTexture() {
        return this.valid ? this.texture : 0;
    }

    public void invalidate() {
        this.valid = false;
    }

    /**
     * Copies from the bound read framebuffer.
     */
    public void copy(int width, int height) {
        if (this.texture == 0) {
            this.texture = LWJGL.glGenTextures();
        }

        // The texture calls below act on the active unit's binding
        this.glState.bindTexture(DefaultChunkRenderer.SKY_TEXTURE_UNIT, GL11.GL_TEXTURE_2D, this.texture);
        int previous = this.glState.activateTextureUnit(GL13.GL_TEXTURE0 + DefaultChunkRenderer.SKY_TEXTURE_UNIT);

        if (this.width != width || this.height != height) {
            this.width = width;
            this.height = height;
            LWJGL.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, null);
            // texelFetch ignores filtering, but the default mipmapped min filter would leave the texture incomplete
            LWJGL.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            LWJGL.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        }

        LWJGL.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, width, height);
        this.glState.activateTextureUnit(previous);
        this.valid = true;
    }

    public void delete() {
        if (this.texture != 0) {
            this.glState.deleteTexture(this.texture);
            this.texture = 0;
            this.width = 0;
            this.height = 0;
        }

        this.valid = false;
    }
}
