package org.embeddedt.embeddium.impl.gl.state;

import org.taumc.celeritas.lwjgl.GL13;

import static org.taumc.celeritas.lwjgl.LWJGLServiceProvider.LWJGL;

public interface GlStateService {
    /**
     * For renderers that don't cache texture state. These leave unit 0 active, so it is taken to be the previous unit.
     */
    GlStateService UNCACHED = new GlStateService() {
        @Override
        public int activateTextureUnit(int unit) {
            LWJGL.glActiveTexture(unit);
            return GL13.GL_TEXTURE0;
        }

        @Override
        public void bindTexture(int unit, int target, int texture) {
            LWJGL.glActiveTexture(GL13.GL_TEXTURE0 + unit);
            LWJGL.glBindTexture(target, texture);
            LWJGL.glActiveTexture(GL13.GL_TEXTURE0);
        }

        @Override
        public void deleteTexture(int texture) {
            LWJGL.glDeleteTextures(texture);
        }
    };

    /**
     * @param unit the unit, as {@code GL_TEXTURE0 + n}
     * @return the unit that was active before, in the same form
     */
    int activateTextureUnit(int unit);

    /**
     * @param unit the unit, as an index
     */
    void bindTexture(int unit, int target, int texture);

    /**
     * Deletes a texture bound with {@link #bindTexture}, forgetting any cached bindings of it. Otherwise a cache would
     * skip binding a new texture that reuses the name, which some drivers (such as Apple's) do right away.
     */
    void deleteTexture(int texture);
}
