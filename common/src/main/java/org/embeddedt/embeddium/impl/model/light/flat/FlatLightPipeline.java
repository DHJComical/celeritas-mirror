package org.embeddedt.embeddium.impl.model.light.flat;

import lombok.RequiredArgsConstructor;
import org.embeddedt.embeddium.impl.model.light.DiffuseProvider;
import org.embeddedt.embeddium.impl.model.light.LightPipeline;
import org.embeddedt.embeddium.impl.model.light.data.LightDataAccess;
import org.embeddedt.embeddium.impl.model.light.data.QuadLightData;
import org.embeddedt.embeddium.impl.model.quad.ModelQuadView;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFlags;
import org.embeddedt.embeddium.api.util.NormI8;

import java.util.Arrays;

import static org.embeddedt.embeddium.impl.model.light.data.LightDataAccess.*;

/**
 * A light pipeline which implements "classic-style" lighting through simply using the light value of the adjacent
 * block to a face.
 */
@RequiredArgsConstructor
public class FlatLightPipeline implements LightPipeline {
    /**
     * The cache which light data will be accessed from.
     */
    private final LightDataAccess lightCache;

    /**
     * Used to retrieve the directional shading value for quads.
     */
    private final DiffuseProvider diffuseProvider;

    /**
     * Whether or not to even attempt to shade quads using their normals rather than light face.
     */
    private final boolean useQuadNormalsForShading;

    @Override
    public void calculate(ModelQuadView quad, int x, int y, int z, QuadLightData out, ModelQuadFacing cullFace, ModelQuadFacing lightFace, boolean shade, boolean applyAoDepthBlending) {
        int lightmap;

        if (!lightFace.isDirection()) {
            throw new IllegalStateException();
        }

        // To match vanilla behavior, use the cull face if it exists/is available
        if (cullFace.isDirection()) {
            lightmap = getOffsetLightmap(x, y, z, cullFace);
        } else {
            int flags = quad.getFlags();
            // If the face is aligned, use the light data above it
            // To match vanilla behavior, also treat the face as aligned if it is parallel and the block state is a full cube
            if ((flags & ModelQuadFlags.IS_ALIGNED) != 0 || ((flags & ModelQuadFlags.IS_PARALLEL) != 0 && unpackFC(this.lightCache.get(x, y, z)))) {
                lightmap = getOffsetLightmap(x, y, z, lightFace);
            } else {
                lightmap = getEmissiveLightmap(this.lightCache.get(x, y, z));
            }
        }

        Arrays.fill(out.lm, lightmap);

        if ((quad.getFlags() & ModelQuadFlags.IS_VANILLA_SHADED) != 0 || !this.useQuadNormalsForShading) {
            Arrays.fill(out.br, this.diffuseProvider.getDiffuse(lightFace, shade));
        } else {
            this.applySidedBrightnessFromNormals(quad, out, shade);
        }
    }

    public void applySidedBrightnessFromNormals(ModelQuadView quad, QuadLightData out, boolean shade) {
        int normal = quad.getModFaceNormal();
        Arrays.fill(out.br, this.diffuseProvider.getDiffuse(NormI8.unpackX(normal), NormI8.unpackY(normal), NormI8.unpackZ(normal), shade));
    }

    /**
     * When vanilla computes an offset lightmap with flat lighting, it passes the original BlockState but the
     * offset BlockPos to LevelRenderer.getLightColor(BlockAndTintGetter, BlockState, BlockPos).
     * This does not make much sense but fixes certain issues, primarily dark quads on light-emitting blocks
     * behind tinted glass. {@link LightDataAccess} cannot efficiently store lightmaps computed with
     * inconsistent values so this method exists to mirror vanilla behavior as closely as possible.
     */
    private int getOffsetLightmap(int x, int y, int z, ModelQuadFacing face) {
        int word = this.lightCache.get(x, y, z);

        // Check emissivity of the origin state
        if (unpackEM(word)) {
            return LightDataAccess.FULL_BRIGHT;
        }

        // Use world light values from the offset pos, but luminance from the origin pos
        int adjWord = this.lightCache.get(x, y, z, face);
        return LightDataAccess.pack(Math.max(unpackBL(adjWord), unpackLU(word)), unpackSL(adjWord));
    }
}
