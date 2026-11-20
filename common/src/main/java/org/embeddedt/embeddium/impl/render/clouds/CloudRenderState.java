package org.embeddedt.embeddium.impl.render.clouds;

/**
 * Sets up the GL state clouds are drawn with. Platforms implement this so they can use their own render state objects
 * and keep their state caches in sync.
 */
public interface CloudRenderState {
    /**
     * Sets up blending and depth testing and writing. Fancy clouds are drawn with back face culling, fast clouds
     * without.
     */
    void setup(boolean fancy);

    void restore();
}
