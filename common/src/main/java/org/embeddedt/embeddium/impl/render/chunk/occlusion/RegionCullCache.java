package org.embeddedt.embeddium.impl.render.chunk.occlusion;

import org.embeddedt.embeddium.impl.render.viewport.CameraTransform;
import org.embeddedt.embeddium.impl.render.viewport.Viewport;
import org.embeddedt.embeddium.impl.render.viewport.frustum.Frustum;

import java.util.Arrays;

import static org.embeddedt.embeddium.impl.common.util.MathUtil.nearestToZero;
import static org.embeddedt.embeddium.impl.render.chunk.region.RenderRegion.*;

/**
 * Per-search memoization for region-level visibility classification.
 *
 * <p>Sections are grouped into dense 8x4x8-section regions
 * ({@code 128x64x128} blocks). The cache computes a conservative result for
 * each region once per search, then lets {@link OcclusionCuller} avoid the
 * exact distance and frustum tests whenever the region result is conclusive.
 * Region ids are dense and serve directly as cache indices.
 *
 * <p>The classification contract is:
 * <ul>
 *   <li>{@link Frustum#OUTSIDE}: every section is guaranteed to fail the
 *       combined render-distance/frustum visibility test;</li>
 *   <li>{@link Frustum#FULLY_INSIDE}: every section is guaranteed to pass both
 *       tests; and</li>
 *   <li>{@link Frustum#PARTIALLY_INSIDE}: the region bounds are inconclusive,
 *       so the caller must test each section exactly.</li>
 * </ul>
 * The bounds include padding and floating-point slack, so a conclusive result
 * must never incorrectly reject or accept a section.
 *
 * <p>The cache is mutable and intended for one search at a time. Call
 * {@link #begin(Viewport, float, int)} before {@link #classify}; it resets the
 * classification array so no result leaks between searches.
 */
final class RegionCullCache {
    // Sentinel stored in unclassified slots; distinct from every Frustum result.
    private static final byte UNCOMPUTED = -1;

    // Per-section frustum padding is 9.125 - 8 = 1.125 blocks. Add a
    // further 1/32 block to absorb camera-relative floating-point rounding.
    private static final float FRUSTUM_PAD = 1.125f + 0.03125f;

    // Slack for distance bounds; errors at render-distance magnitudes are
    // approximately 1e-4, so this keeps conservative comparisons stable.
    private static final float DISTANCE_EPSILON = 0.001f;

    private static final int INITIAL_CAPACITY = 64;

    // Indexed by dense region id; UNCOMPUTED until classified in the current search.
    private byte[] classification = new byte[INITIAL_CAPACITY];

    // Search configuration, refreshed by begin().
    private Viewport viewport;
    private CameraTransform camera;
    private float maxDistance;

    /**
     * Start a search and configure the camera-relative bounds used by
     * subsequent classifications.
     *
     * @param viewport current camera and frustum
     * @param searchDistance maximum horizontal/vertical render distance
     * @param numRegions exclusive upper bound of the dense region ids that
     *                    may be passed to {@link #classify}
     */
    void begin(Viewport viewport, float searchDistance, int numRegions) {
        this.viewport = viewport;
        this.camera = viewport.getTransform();
        this.maxDistance = searchDistance;
        if (numRegions > this.classification.length) {
            this.grow(numRegions);
        }
        Arrays.fill(this.classification, 0, numRegions, UNCOMPUTED);
    }

    /**
     * Return the cached classification for a region, computing it on first
     * use in the current search.
     *
     * @param regionId dense region id and cache slot
     * @param regionOriginX block X coordinate of the region's minimum corner
     * @param regionOriginY block Y coordinate of the region's minimum corner
     * @param regionOriginZ block Z coordinate of the region's minimum corner
     * @return one of the {@link Frustum} classification constants
     * @implNote The origin must correspond to {@code regionId}; only the first
     *           classification for an id is computed in a search.
     */
    int classify(int regionId, int regionOriginX, int regionOriginY, int regionOriginZ) {

        byte result = this.classification[regionId];

        if (result == UNCOMPUTED) {
            result = (byte) this.compute(regionOriginX, regionOriginY, regionOriginZ);
            this.classification[regionId] = result;
        }

        return result;
    }

    // Grow the classification array; contents are reset by the next begin().
    private void grow(int minimumCapacity) {
        int capacity = Math.max(minimumCapacity, this.classification.length * 2);
        this.classification = new byte[capacity];
    }

    /**
     * Compute conservative region bounds. Distance is rejected first when the
     * nearest possible section point is too far away; otherwise the padded
     * region box is classified against the camera frustum.
     */
    private int compute(int regionOriginX, int regionOriginY, int regionOriginZ) {
        final CameraTransform t = this.camera;
        final float maxDistance = this.maxDistance;

        final int ax = regionOriginX - t.intX, bx = ax + REGION_BLOCK_WIDTH;
        final int ay = regionOriginY - t.intY, by = ay + REGION_BLOCK_HEIGHT;
        final int az = regionOriginZ - t.intZ, bz = az + REGION_BLOCK_LENGTH;

        // Signed range of the nearest-point distance component for all member
        // section boxes along each axis.
        final float loX = nearestToZero(ax, ax + 16) - t.fracX;
        final float hiX = nearestToZero(bx - 16, bx) - t.fracX;
        final float loY = nearestToZero(ay, ay + 16) - t.fracY;
        final float hiY = nearestToZero(by - 16, by) - t.fracY;
        final float loZ = nearestToZero(az, az + 16) - t.fracZ;
        final float hiZ = nearestToZero(bz - 16, bz) - t.fracZ;

        // Upper bound on every member's absolute distance component.
        final float farX = maxAbs(loX, hiX) + DISTANCE_EPSILON;
        final float farY = maxAbs(loY, hiY) + DISTANCE_EPSILON;
        final float farZ = maxAbs(loZ, hiZ) + DISTANCE_EPSILON;

        // Lower bound on every member's absolute distance component.
        final float nearX = Math.max(0.0f, minAbs(loX, hiX) - DISTANCE_EPSILON);
        final float nearY = Math.max(0.0f, minAbs(loY, hiY) - DISTANCE_EPSILON);
        final float nearZ = Math.max(0.0f, minAbs(loZ, hiZ) - DISTANCE_EPSILON);

        final float maxDistanceSq = maxDistance * maxDistance;

        // If even the nearest possible point fails the distance test, every
        // section is out of range and the frustum need not be queried.
        boolean distanceOut = ((nearX * nearX) + (nearZ * nearZ)) >= maxDistanceSq || nearY >= maxDistance;

        if (distanceOut) {
            return Frustum.OUTSIDE;
        }

        // Only claim FULLY_INSIDE when the farthest possible point is still
        // strictly inside the distance bounds.
        boolean distanceIn = ((farX * farX) + (farZ * farZ)) < maxDistanceSq && farY < maxDistance;

        int result = this.viewport.intersectCameraRelativeBox(
                (ax - t.fracX) - FRUSTUM_PAD,
                (ay - t.fracY) - FRUSTUM_PAD,
                (az - t.fracZ) - FRUSTUM_PAD,
                (bx - t.fracX) + FRUSTUM_PAD,
                (by - t.fracY) + FRUSTUM_PAD,
                (bz - t.fracZ) + FRUSTUM_PAD);

        if (result == Frustum.FULLY_INSIDE) {
            // A fully-inside frustum result is conclusive only when distance
            // is also conclusive for every member section.
            return distanceIn ? Frustum.FULLY_INSIDE : Frustum.PARTIALLY_INSIDE;
        } else {
            return result;
        }
    }

    // Largest absolute value in a signed interval.
    private static float maxAbs(float lo, float hi) {
        return Math.max(Math.abs(lo), Math.abs(hi));
    }

    // Smallest absolute value in a signed interval.
    private static float minAbs(float lo, float hi) {
        if (lo > 0.0f) {
            return lo;
        }

        if (hi < 0.0f) {
            return -hi;
        }

        return 0.0f;
    }
}
