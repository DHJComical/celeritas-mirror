package org.embeddedt.embeddium.impl.render.chunk.occlusion;

import grondag.bitraster.PackedBox;
import org.embeddedt.embeddium.impl.render.viewport.Viewport;
import org.embeddedt.embeddium.impl.render.viewport.frustum.SimpleFrustum;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A section seen only through a thin gap between two occluders at different depths, as where the edges of two
 * walls nearly line up on screen.
 *
 * <p>The camera looks down +Z from {@code x = -e}. A near occluder fills world x in [-16, 0] at z in [32, 48],
 * so its silhouette edge on the gap side is its front edge, at tangent {@code e / 32}. A far occluder fills x in
 * [1, 16] at z in [64, 80], so its edge on the gap side is its back edge, at tangent {@code (1 + e) / 80}. The
 * rays between the two edges pass both occluders and hit a wall section at z in [96, 112] that the occluders
 * otherwise hide completely, so the wall is visible and a correct culler must not report it hidden. Moving the
 * camera along x changes the gap width, which is {@code 1/80 - 3e/160}.
 */
class RasterOccluderSliverTest {
    private static final float FOV_DEGREES = 70.0f;
    private static final float ASPECT = 16.0f / 9.0f;
    private static final float TEST_DISTANCE = 128.0f;

    private static final int[] NEAR_OCCLUDER = section(PackedBox.pack(0, 0, 0, 16, 16, 16, PackedBox.RANGE_EXTREME));
    private static final int[] FAR_OCCLUDER = section(PackedBox.pack(1, 0, 0, 16, 16, 16, PackedBox.RANGE_EXTREME));

    private static int[] section(int box) {
        // data[0] is the renderable bounds and the occluder boxes follow; see SectionVisibilityBuilder
        return new int[] { box, box };
    }

    private static Viewport viewport(double cameraX) {
        Matrix4f matrix = new Matrix4f()
                .perspective((float) Math.toRadians(FOV_DEGREES), ASPECT, 0.05f, 512.0f)
                .lookAlong(0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f);

        return new Viewport(new SimpleFrustum(new FrustumIntersection(matrix)), new Vector3d(cameraX, 8.0, 0.0), matrix);
    }

    /** Width of the gap in tangent units, i.e. on a screen plane one block in front of the camera. */
    private static double gapTangent(double e) {
        return ((1.0 + e) / 80.0) - (e / 32.0);
    }

    /** Camera offset that opens a gap of the given width in tangent units. */
    private static double offsetForGap(double gapTangent) {
        return (1.0 / 80.0 - gapTangent) * 160.0 / 3.0;
    }

    /** Width of one buffer pixel in tangent units, for the buffer {@link RasterOccluder} sizes for this view. */
    private static double pixelTangent() {
        RasterOccluder occluder = new RasterOccluder();
        occluder.prepareScene(1, viewport(0.0), TEST_DISTANCE);
        double halfWidth = ASPECT * Math.tan(Math.toRadians(FOV_DEGREES / 2.0));
        return 2.0 * halfWidth / occluder.bufferWidth();
    }

    private static RasterOccluder.SectionVisibility testWall(double e, boolean drawNear, boolean drawFar) {
        Viewport viewport = viewport(-e);
        RasterOccluder occluder = new RasterOccluder();
        occluder.prepareScene(1, viewport, TEST_DISTANCE);

        // both occluders are fewer hops from the camera than the wall, so the search draws them first
        if (drawNear) {
            occluder.occludeSectionAt(-16, 0, 32, NEAR_OCCLUDER);
        }

        if (drawFar) {
            occluder.occludeSectionAt(0, 0, 64, FAR_OCCLUDER);
        }

        // camera is in chunk (-1, 0, 0) and the wall in chunk (0, 0, 6)
        return occluder.testSection(0, 0, 96, 1 + 36, PackedBox.FULL_BOX);
    }

    /** The scene itself: with either occluder missing the wall is plainly visible. */
    @Test
    void wallVisibleWithoutBothOccluders() {
        assertEquals(RasterOccluder.SectionVisibility.VISIBLE, testWall(0.6, false, false));
        assertEquals(RasterOccluder.SectionVisibility.VISIBLE, testWall(0.6, true, false));
        assertEquals(RasterOccluder.SectionVisibility.VISIBLE, testWall(0.6, false, true));
    }

    /** The scene itself: once the far occluder's edge passes behind the near one, the wall really is hidden. */
    @Test
    void wallHiddenWhenGapCloses() {
        assertTrue(gapTangent(0.9) < 0);
        assertEquals(RasterOccluder.SectionVisibility.HIDDEN, testWall(0.9, true, true));
    }

    /**
     * A gap at least a buffer pixel wide always holds a pixel centre, so occluders drawn by pixel-centre sampling
     * can never close it, wherever it falls. Narrower gaps can close when no centre falls inside them; that is a
     * limit of centre sampling, not tested here.
     */
    @Test
    void gapOfOnePixelOrMoreStaysOpen() {
        double pixel = pixelTangent();
        int steps = 64;
        List<String> culled = new ArrayList<>();

        for (int i = 0; i < steps; i++) {
            double pixels = 1.0 + ((double) i / (steps - 1));
            double e = offsetForGap(pixels * pixel);
            var result = testWall(e, true, true);

            if (result != RasterOccluder.SectionVisibility.VISIBLE) {
                culled.add(String.format("gap=%.3f px (e=%.4f) -> %s", pixels, e, result));
            }
        }

        assertTrue(culled.isEmpty(), culled.size() + "/" + steps + " gaps of 1-2 buffer pixels culled a visible section:\n"
                + String.join("\n", culled));
    }
}
