package org.embeddedt.embeddium.impl.render.chunk.fog;

import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderComponent;

public interface FogService {
    // Keep in sync with fog.glsl
    int FOG_SHAPE_SPHERICAL = 0;
    int FOG_SHAPE_CYLINDRICAL = 1;
    int FOG_SHAPE_PLANAR = 2;

    float getFogEnd();
    float getFogStart();
    float getFogDensity();
    int getFogShapeIndex();
    float getFogCutoff();
    float[] getFogColor();

    ChunkShaderComponent.Factory<?> getFogMode();

    /**
     * {@return the GL handle for a viewport-sized texture with the sky, or 0 when not available}
     */
    default int getSkyTexture() {
        return 0;
    }

    default float getSkyFogEdgeStart() {
        return this.getFogStart();
    }

    default float getSkyFogEdgeEnd() {
        return this.getFogEnd();
    }
}
