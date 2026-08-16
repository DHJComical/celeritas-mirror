package org.embeddedt.embeddium.impl.render.chunk.occlusion;

import org.embeddedt.embeddium.impl.render.viewport.CameraTransform;
import org.embeddedt.embeddium.impl.render.viewport.Viewport;
import org.embeddedt.embeddium.impl.render.viewport.frustum.Frustum;

import java.util.Arrays;

import static org.embeddedt.embeddium.impl.common.util.MathUtil.nearestToZero;
import static org.embeddedt.embeddium.impl.render.chunk.region.RenderRegion.*;

/**
 * Per-region memoization of the two per-section visibility predicates used by {@link OcclusionCuller}.
 * <p>
 * Sections are grouped into 8x4x8-section regions (128x64x128 blocks) with dense ids. Once per frame a region
 * is classified as:
 * <ul>
 *   <li>{@link Frustum#OUTSIDE} - <i>every</i> section in the region is guaranteed to fail
 *       {@code isWithinRenderDistance(..) &amp;&amp; isWithinFrustum(..)}</li>
 *   <li>{@link Frustum#FULLY_INSIDE} - <i>every</i> section in the region is guaranteed to pass both</li>
 *   <li>{@link Frustum#PARTIALLY_INSIDE} - unknown; the caller must run the exact per-section tests</li>
 * </ul>
 */
final class RegionCullCache {
    /** 1.125 (= 9.125 - 8) plus 1/32 of slack to swallow float rounding in the camera-relative transform. */
    private static final float FRUSTUM_PAD = 1.125f + 0.03125f;

    /** Slack on the distance bounds; float error at these magnitudes is on the order of 1e-4. */
    private static final float DISTANCE_EPSILON = 0.001f;

    private static final int INITIAL_CAPACITY = 64;

    private byte[] classification = new byte[INITIAL_CAPACITY];
    private int[] stamp = new int[INITIAL_CAPACITY];

    private int currentStamp;

    // Per-frame state, refreshed by begin().
    private Viewport viewport;
    private CameraTransform camera;
    private float maxDistance;

    RegionCullCache() {
        Arrays.fill(this.stamp, -1);
    }

    void begin(Viewport viewport, float searchDistance, int numRegions) {
        this.viewport = viewport;
        this.camera = viewport.getTransform();
        this.maxDistance = searchDistance;
        this.currentStamp++;
        if (numRegions >= this.stamp.length) {
            this.grow(numRegions);
        }
    }

    /**
     * @param regionId      dense region id; used only as a memoization slot
     * @param regionOriginX block coordinate of the region's minimum corner
     */
    int classify(int regionId, int regionOriginX, int regionOriginY, int regionOriginZ) {

        if (this.stamp[regionId] != this.currentStamp) {
            this.stamp[regionId] = this.currentStamp;
            this.classification[regionId] = (byte) this.compute(regionOriginX, regionOriginY, regionOriginZ);
        }

        return this.classification[regionId];
    }

    private void grow(int minimumCapacity) {
        int capacity = Math.max(minimumCapacity, this.stamp.length * 2);

        int oldLength = this.stamp.length;

        this.stamp = Arrays.copyOf(this.stamp, capacity);
        this.classification = Arrays.copyOf(this.classification, capacity);

        Arrays.fill(this.stamp, oldLength, capacity, -1);
    }

    private int compute(int regionOriginX, int regionOriginY, int regionOriginZ) {
        final CameraTransform t = this.camera;
        final float maxDistance = this.maxDistance;

        final int ax = regionOriginX - t.intX, bx = ax + REGION_BLOCK_WIDTH;
        final int ay = regionOriginY - t.intY, by = ay + REGION_BLOCK_HEIGHT;
        final int az = regionOriginZ - t.intZ, bz = az + REGION_BLOCK_LENGTH;

        // Signed range of the per-axis "nearest point of a member section's box" value.
        final float loX = nearestToZero(ax, ax + 16) - t.fracX;
        final float hiX = nearestToZero(bx - 16, bx) - t.fracX;
        final float loY = nearestToZero(ay, ay + 16) - t.fracY;
        final float hiY = nearestToZero(by - 16, by) - t.fracY;
        final float loZ = nearestToZero(az, az + 16) - t.fracZ;
        final float hiZ = nearestToZero(bz - 16, bz) - t.fracZ;

        // Upper bound on every member's |d|, inflated by epsilon.
        final float farX = maxAbs(loX, hiX) + DISTANCE_EPSILON;
        final float farY = maxAbs(loY, hiY) + DISTANCE_EPSILON;
        final float farZ = maxAbs(loZ, hiZ) + DISTANCE_EPSILON;

        // Lower bound on every member's |d|, deflated by epsilon.
        final float nearX = Math.max(0.0f, minAbs(loX, hiX) - DISTANCE_EPSILON);
        final float nearY = Math.max(0.0f, minAbs(loY, hiY) - DISTANCE_EPSILON);
        final float nearZ = Math.max(0.0f, minAbs(loZ, hiZ) - DISTANCE_EPSILON);

        final float maxDistanceSq = maxDistance * maxDistance;

        boolean distanceOut = ((nearX * nearX) + (nearZ * nearZ)) >= maxDistanceSq || nearY >= maxDistance;

        if (distanceOut) {
            return Frustum.OUTSIDE;
        }

        boolean distanceIn = ((farX * farX) + (farZ * farZ)) < maxDistanceSq && farY < maxDistance;

        int result = this.viewport.intersectCameraRelativeBox(
                (ax - t.fracX) - FRUSTUM_PAD,
                (ay - t.fracY) - FRUSTUM_PAD,
                (az - t.fracZ) - FRUSTUM_PAD,
                (bx - t.fracX) + FRUSTUM_PAD,
                (by - t.fracY) + FRUSTUM_PAD,
                (bz - t.fracZ) + FRUSTUM_PAD);

        if (result == Frustum.FULLY_INSIDE) {
            // Region must be fully within render distance to propagate that result
            return distanceIn ? Frustum.FULLY_INSIDE : Frustum.PARTIALLY_INSIDE;
        } else {
            return result;
        }
    }

    private static float maxAbs(float lo, float hi) {
        return Math.max(Math.abs(lo), Math.abs(hi));
    }

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
