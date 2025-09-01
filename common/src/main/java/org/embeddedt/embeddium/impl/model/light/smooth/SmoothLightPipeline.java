package org.embeddedt.embeddium.impl.model.light.smooth;

import org.embeddedt.embeddium.impl.common.util.MathUtil;
import org.embeddedt.embeddium.impl.model.light.DiffuseProvider;
import org.embeddedt.embeddium.impl.model.light.LightPipeline;
import org.embeddedt.embeddium.impl.model.light.data.LightDataAccess;
import org.embeddedt.embeddium.impl.model.light.data.QuadLightData;
import org.embeddedt.embeddium.impl.model.quad.ModelQuadView;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFlags;
import org.embeddedt.embeddium.api.util.NormI8;
import org.embeddedt.embeddium.impl.util.PositionUtil;

/**
 * A light pipeline which produces smooth interpolated lighting and ambient occlusion for model quads. This
 * implementation makes a number of improvements over vanilla's own "smooth lighting" option. In no particular order:
 *
 * - Corner blocks are now selected from the correct set of neighbors above block faces (fixes MC-148689 and MC-12558)
 * - Shading issues caused by anisotropy are fixed by re-orientating quads to a consistent ordering (fixes MC-138211)
 * - Blocks next to emissive blocks are too bright (MC-260989)
 * - Synchronization issues between the main render thread's light engine and chunk build worker threads are corrected
 *   by copying light data alongside block states, fixing a number of inconsistencies in baked chunks (no open issue)
 *
 * This implementation also includes a significant number of optimizations:
 *
 * - Computed light data for a given block face is cached and re-used again when multiple quads exist for a given
 *   facing, making complex block models less expensive to render
 * - The light data cache encodes as much information as possible into integer words to improve cache locality and
 *   to eliminate the multiple array lookups that would otherwise be needed, significantly speeding up this section
 * - Block faces aligned to the block grid use a fast-path for mapping corner light values to vertices without expensive
 *   interpolation or blending, speeding up most block renders
 * - Some critical code paths have been re-written to hit the JVM's happy path, allowing it to perform auto-vectorization
 *   of the blend functions
 * - Information about a given model quad is cached to enable the light pipeline to make certain assumptions and skip
 *   unnecessary computation
 */
public class SmoothLightPipeline implements LightPipeline {
    /**
     * The cache which light data will be accessed from.
     */
    private final LightDataAccess lightCache;

    /**
     * The cached face data for each side of a block, both inset and outset.
     */
    private final AoFaceData[] cachedFaceData = new AoFaceData[6 * 2];

    /**
     * The position at which the cached face data was taken at.
     */
    private long cachedPos = Long.MIN_VALUE;

    /**
     * A temporary array for storing the intermediary results of weight data for non-aligned face blending.
     */
    private final float[] weights = new float[4];

    /**
     * Whether or not to even attempt to shade quads using their normals rather than light face.
     */
    private final boolean useQuadNormalsForShading;

    /**
     * Used to retrieve the directional shading value for quads.
     */
    private final DiffuseProvider diffuseProvider;

    private float lastAo, lastBl, lastSl;

    public SmoothLightPipeline(LightDataAccess cache, DiffuseProvider diffuseProvider, boolean useQuadNormalsForShading) {
        this.lightCache = cache;

        for (int i = 0; i < this.cachedFaceData.length; i++) {
            this.cachedFaceData[i] = new AoFaceData();
        }

        this.useQuadNormalsForShading = useQuadNormalsForShading;
        this.diffuseProvider = diffuseProvider;
    }

    @Override
    public void calculate(ModelQuadView quad, int x, int y, int z, QuadLightData out, ModelQuadFacing cullFace, ModelQuadFacing lightFace,
                          boolean shade, boolean applyAoDepthBlending) {
        this.updateCachedData(PositionUtil.packBlock(x, y, z));

        int flags = quad.getFlags();

        final AoNeighborInfo neighborInfo = AoNeighborInfo.get(lightFace);

        // If the model quad is aligned to the block's face and covers it entirely, we can take a fast path and directly
        // map the corner values onto this quad's vertices. This covers most situations during rendering and provides
        // a modest speed-up.
        // To match vanilla behavior, also treat the face as aligned if it is parallel and the block state is a full cube
        if ((flags & ModelQuadFlags.IS_ALIGNED) != 0 || ((flags & ModelQuadFlags.IS_PARALLEL) != 0 && LightDataAccess.unpackFC(this.lightCache.get(x, y, z)))) {
            if ((flags & ModelQuadFlags.IS_PARTIAL) == 0) {
                this.applyAlignedFullFace(neighborInfo, x, y, z, lightFace, out);
            } else {
                this.applyAlignedPartialFace(neighborInfo, quad, x, y, z, lightFace, out);
            }
        } else {
            if ((flags & ModelQuadFlags.IS_VANILLA_SHADED) == 0 && quad.getNormalFace() == ModelQuadFacing.UNASSIGNED) {
                // Normal has multiple nonzero components
                this.applyIrregularFace(quad, x, y, z, out, applyAoDepthBlending);
            } else {
                // Normal has a single nonzero component
                this.applyNonParallelFace(neighborInfo, quad, x, y, z, lightFace, out, applyAoDepthBlending);
            }
        }

        if((flags & ModelQuadFlags.IS_VANILLA_SHADED) != 0 || !this.useQuadNormalsForShading) {
            this.applySidedBrightness(out, lightFace, shade);
        } else {
            this.applySidedBrightnessFromNormals(out, quad, shade);
        }
    }

    @Override
    public void reset() {
        this.cachedPos = Long.MIN_VALUE;
    }

    /**
     * Quickly calculates the light data for a full grid-aligned quad. This represents the most common case (outward
     * facing quads on a full-block model) and avoids interpolation between neighbors as each corner will only ever
     * have two contributing sides.
     * Flags: IS_ALIGNED, !IS_PARTIAL
     */
    private void applyAlignedFullFace(AoNeighborInfo neighborInfo, int x, int y, int z, ModelQuadFacing dir, QuadLightData out) {
        AoFaceData faceData = this.getCachedFaceData(x, y, z, dir, true);
        neighborInfo.mapCorners(faceData.lm, faceData.ao, out.lm, out.br);
    }

    /**
     * Calculates the light data for a grid-aligned quad that does not cover the entire block volume's face.
     * Flags: IS_ALIGNED, IS_PARTIAL
     */
    private void applyAlignedPartialFace(AoNeighborInfo neighborInfo, ModelQuadView quad, int x, int y, int z, ModelQuadFacing dir, QuadLightData out) {
        for (int i = 0; i < 4; i++) {
            // Clamp the vertex positions to the block's boundaries to prevent weird errors in lighting
            float cx = clamp(quad.getX(i));
            float cy = clamp(quad.getY(i));
            float cz = clamp(quad.getZ(i));

            float[] weights = this.weights;
            neighborInfo.calculateCornerWeights(cx, cy, cz, weights);
            this.applyAlignedPartialFaceVertex(x, y, z, dir, weights, true);
            out.br[i] = lastAo;
            out.lm[i] = getLightMapCoord(lastSl, lastBl);
        }
    }

    /**
     * Flags: !IS_ALIGNED, !IS_PARALLEL
     */
    private void applyNonParallelFace(AoNeighborInfo neighborInfo, ModelQuadView quad, int x, int y, int z, ModelQuadFacing dir,
                                      QuadLightData out, boolean applyAoDepthBlending) {
        for (int i = 0; i < 4; i++) {
            // Clamp the vertex positions to the block's boundaries to prevent weird errors in lighting
            float cx = clamp(quad.getX(i));
            float cy = clamp(quad.getY(i));
            float cz = clamp(quad.getZ(i));

            float[] weights = this.weights;
            neighborInfo.calculateCornerWeights(cx, cy, cz, weights);

            float depth = neighborInfo.getDepth(cx, cy, cz);

            if (applyAoDepthBlending) {
                // Blend the occlusion factor between the blocks directly beside this face and the blocks above it
                // based on how inset the face is. This fixes a few issues with blocks such as farmland and paths.
                this.applyInsetPartialFaceVertex(x, y, z, dir, depth, 1.0f - depth, weights);
            } else {
                this.applyAlignedPartialFaceVertex(x, y, z, dir, weights, MathUtil.roughlyEqual(depth, 0.0F));
            }

            out.br[i] = lastAo;
            out.lm[i] = getLightMapCoord(lastSl, lastBl);
        }
    }

    private void applyInsetPartialFaceVertex(int x, int y, int z, ModelQuadFacing dir, float n1d, float n2d, float[] w) {
        // Avoid blending when the depth is close to one value or the other
        if (MathUtil.roughlyEqual(n1d, 0.0f)) {
            this.applyAlignedPartialFaceVertex(x, y, z, dir, w, true);
            return;
        } else if (MathUtil.roughlyEqual(n1d, 1.0f)) {
            this.applyAlignedPartialFaceVertex(x, y, z, dir, w, false);
            return;
        }

        AoFaceData n1 = this.getCachedFaceData(x, y, z, dir, false);
        if (!n1.hasUnpackedLightData()) {
            n1.unpackLightData();
        }

        AoFaceData n2 = this.getCachedFaceData(x, y, z, dir, true);
        if (!n2.hasUnpackedLightData()) {
            n2.unpackLightData();
        }

        // Blend between the direct neighbors and above based on the passed weights
        this.lastAo = (n1.getBlendedShade(w) * n1d) + (n2.getBlendedShade(w) * n2d);
        this.lastSl = (n1.getBlendedSkyLight(w) * n1d) + (n2.getBlendedSkyLight(w) * n2d);
        this.lastBl = (n1.getBlendedBlockLight(w) * n1d) + (n2.getBlendedBlockLight(w) * n2d);
    }

    private static final float BLENDED_WEIGHT = 0.75f;
    private static final float MAX_WEIGHT = 1f - BLENDED_WEIGHT;

    private void applyIrregularFace(ModelQuadView quad, int x, int y, int z, QuadLightData out, boolean applyAoDepthBlending) {
        for (int i = 0; i < 4; i++) {
            // Clamp the vertex positions to the block's boundaries to prevent weird errors in lighting
            float cx = clamp(quad.getX(i));
            float cy = clamp(quad.getY(i));
            float cz = clamp(quad.getZ(i));

            int normal = quad.getForgeNormal(i);
            if (normal == 0) {
                normal = quad.getComputedFaceNormal();
            }

            float weightedAo = 0, weightedBl = 0, weightedSl = 0;
            float maxAo = 0, maxBl = 0, maxSl = 0;

            // Compute the values that each axis would contribute, then combine them
            for (int axis = 0; axis < 3; axis++) {
                // Unpack the desired normal component
                float projectedNormal = NormI8.unpackX(normal >> (axis * 8));

                // Skip any components that are zero (they will never contribute anything)
                if (projectedNormal == 0) {
                    continue;
                }

                var dir = ModelQuadFacing.AXES[axis].getFacing(projectedNormal > 0);

                var neighborInfo = AoNeighborInfo.get(dir);

                float[] weights = this.weights;
                neighborInfo.calculateCornerWeights(cx, cy, cz, weights);

                float depth = neighborInfo.getDepth(cx, cy, cz);

                if (applyAoDepthBlending) {
                    // Blend the occlusion factor between the blocks directly beside this face and the blocks above it
                    // based on how inset the face is. This fixes a few issues with blocks such as farmland and paths.
                    this.applyInsetPartialFaceVertex(x, y, z, dir, depth, 1.0f - depth, weights);
                } else {
                    // Use inset data as soon as the face is even partially inset
                    this.applyAlignedPartialFaceVertex(x, y, z, dir, weights, MathUtil.roughlyEqual(depth, 0.0F));
                }

                float ao = this.lastAo, sl = this.lastSl, bl = this.lastBl;

                float combineWeight = projectedNormal * projectedNormal;

                weightedAo += ao * combineWeight;
                weightedBl += bl * combineWeight;
                weightedSl += sl * combineWeight;

                maxAo = Math.max(ao, maxAo);
                maxSl = Math.max(sl, maxSl);
                maxBl = Math.max(bl, maxBl);
            }

            out.br[i] = clamp(weightedAo * BLENDED_WEIGHT + maxAo * MAX_WEIGHT);
            out.lm[i] = getLightMapCoord(weightedSl * BLENDED_WEIGHT + maxSl * MAX_WEIGHT, weightedBl * BLENDED_WEIGHT + maxBl * MAX_WEIGHT);
        }
    }

    private void applyAlignedPartialFaceVertex(int x, int y, int z, ModelQuadFacing dir, float[] w, boolean offset) {
        AoFaceData faceData = this.getCachedFaceData(x, y, z, dir, offset);

        if (!faceData.hasUnpackedLightData()) {
            faceData.unpackLightData();
        }

        this.lastSl = faceData.getBlendedSkyLight(w);
        this.lastBl = faceData.getBlendedBlockLight(w);
        this.lastAo = faceData.getBlendedShade(w);
    }

    private void applySidedBrightness(QuadLightData out, ModelQuadFacing face, boolean shade) {
        float brightness = this.diffuseProvider.getDiffuse(face, shade);
        float[] br = out.br;

        for (int i = 0; i < br.length; i++) {
            br[i] *= brightness;
        }
    }

    private void applySidedBrightnessFromNormals(QuadLightData out, ModelQuadView quad, boolean shade) {
        // TODO: consider calculating for vertex if mods actually change normals per-vertex
        int normal = quad.getModFaceNormal();
        float brightness = this.diffuseProvider.getDiffuse(NormI8.unpackX(normal), NormI8.unpackY(normal), NormI8.unpackZ(normal), shade);
        float[] br = out.br;

        for (int i = 0; i < br.length; i++) {
            br[i] *= brightness;
        }
    }

    /**
     * Returns the cached data for a given facing or calculates it if it hasn't been cached.
     */
    private AoFaceData getCachedFaceData(int x, int y, int z, ModelQuadFacing face, boolean offset) {
        AoFaceData data = this.cachedFaceData[offset ? face.ordinal() : face.ordinal() + 6];

        if (!data.hasLightData()) {
            data.initLightData(this.lightCache, x, y, z, face, offset);
        }

        return data;
    }

    private void updateCachedData(long key) {
        if (this.cachedPos != key) {
            for (AoFaceData data : this.cachedFaceData) {
                data.reset();
            }

            this.cachedPos = key;
        }
    }

    /**
     * Clamps the given float to the range [0.0, 1.0].
     */
    private static float clamp(float v) {
        if (v < 0.0f) {
            return 0.0f;
        } else if (v > 1.0f) {
            return 1.0f;
        }

        return v;
    }

    /**
     * Returns a texture coordinate on the light map texture for the given block and sky light values.
     */
    private static int getLightMapCoord(float sl, float bl) {
        return (((int) sl & 0xFF) << 16) | ((int) bl & 0xFF);
    }

}