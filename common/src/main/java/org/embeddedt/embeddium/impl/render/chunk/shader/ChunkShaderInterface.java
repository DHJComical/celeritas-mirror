package org.embeddedt.embeddium.impl.render.chunk.shader;

import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.joml.Matrix4fc;

public interface ChunkShaderInterface {
    void setupState(TerrainRenderPass pass);
    default void restoreState() {}
    void setProjectionMatrix(Matrix4fc matrix);
    void setModelViewMatrix(Matrix4fc matrix);
    void setRegionOffset(float x, float y, float z);
}
