package org.embeddedt.embeddium.impl.model.light;

import org.embeddedt.embeddium.impl.model.light.data.QuadLightData;
import org.embeddedt.embeddium.impl.model.quad.ModelQuadView;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.jetbrains.annotations.NotNull;

/**
 * Light pipelines allow model quads for any location in the world to be lit regardless of what produced them
 * (blocks, fluids, or block entities).
 */
public interface LightPipeline {
    /**
     * Calculates the light data for a given block model quad, storing the result in {@param out}.
     * @param quad The block model quad
     * @param x x-coordinate of the model this quad belongs to
     * @param y y-coordinate of the model this quad belongs to
     * @param z z-coordinate of the model this quad belongs to
     * @param out The data arrays which will store the calculated light data results
     * @param cullFace The cull face of the quad, may be {@link ModelQuadFacing#UNASSIGNED} if there is none
     * @param lightFace The light face of the quad, must not be {@link ModelQuadFacing#UNASSIGNED}
     * @param shade True if the block is shaded by ambient occlusion
     */
    void calculate(ModelQuadView quad, int x, int y, int z, QuadLightData out, @NotNull ModelQuadFacing cullFace, @NotNull ModelQuadFacing lightFace, boolean shade);

    /**
     * Reset any cached data for this pipeline.
     */
    default void reset() {

    }
}
