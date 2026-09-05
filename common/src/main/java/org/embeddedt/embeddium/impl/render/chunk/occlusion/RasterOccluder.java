package org.embeddedt.embeddium.impl.render.chunk.occlusion;

import grondag.bitraster.AbstractRasterizer;
import grondag.bitraster.BoxOccluder;
import grondag.bitraster.Constants;
import grondag.bitraster.Matrix4L;
import grondag.bitraster.PackedBox;
import grondag.bitraster.PerspectiveRasterizer;
import org.embeddedt.embeddium.impl.render.chunk.region.RenderRegion;
import org.embeddedt.embeddium.impl.render.viewport.Viewport;
import org.joml.Matrix4fc;

import java.util.Arrays;

public final class RasterOccluder extends BoxOccluder {
    /** Sections this close are drawn but never tested, since they're almost always visible and most costly to test */
    public static final int NEAR_SQUARED_CHUNK_DIST = 3;

    private int backtrackCount;

    /**
     * Per-region screen bounds for the current scene, so that sections of a region whose screen area has
     * had nothing drawn in it yet are visible without a test of their own. Most of the sections a surface
     * camera reaches are sky, and nothing ever gets drawn above the horizon, so this resolves most of them.
     *
     * <p>{@code regionScene} stamps the scene the bounds were computed for. {@code regionVersion} holds the
     * touched-map version at which the bounds were last found untouched, or {@link #REGION_TOUCHED} once they
     * were not: drawing only ever adds coverage, so that verdict is final for the scene. A region that is not
     * wholly on screen keeps the clip-space position of its first section centre in {@code regionClip}, from
     * which any section centre's on-screen test is a few multiplies.
     */
    private int[] regionScene = new int[0];
    private int[] regionTiles = new int[0];
    private int[] regionVersion = new int[0];
    private byte[] regionState = new byte[0];
    private float[] regionClip = new float[0];
    private int scene;

    /** {@code regionVersion} values that never match a touched-map version. */
    private static final int REGION_TOUCHED = -1, REGION_UNVERIFIED = -2;

    /** The region reaches the near plane or lies wholly off screen; its sections are all tested. */
    private static final byte REGION_NO_BOUNDS = 0;
    /** The region straddles a screen edge; each section centre is checked against the screen. */
    private static final byte REGION_PARTLY_ON_SCREEN = 1;
    /** Every point of the region is on screen. */
    private static final byte REGION_ON_SCREEN = 2;

    /** Work counters for benchmarks; only maintained when {@link AbstractRasterizer#STATS} is set. */
    public static long STAT_CENTER_HIT, STAT_AIR_TESTS, STAT_AIR_VISIBLE, STAT_NEAR, STAT_REGION_SKIP;
    public static long STAT_TEST_NANOS, STAT_OCCLUDE_NANOS, STAT_SECTIONS, STAT_OCCLUDED_SECTIONS;

    public enum SectionVisibility {
        /** nothing of the section is visible, and nothing behind it can be reached through it */
        HIDDEN,
        /** the section draws nothing visible, but the space it occupies is not fully covered, so
         *  traversal must continue through it or geometry behind it would be lost */
        TRAVERSABLE,
        /** the section is visible and can be used to occlude other sections */
        VISIBLE
    }

    public RasterOccluder() {
        super(new PerspectiveRasterizer());
    }

    @Override
    public boolean isBoxVisible(int packedBox, int fuzz) {
        return isBoxVisibleFromPerspective(packedBox, fuzz);
    }

    @Override
    public void occludeBox(int packedBox) {
        occludeFromPerspective(packedBox);
    }

    /**
     * Clears the buffer for a new search. The matrix already carries rotation and projection but no
     * camera translation, so it is supplied as the projection with an identity model matrix;
     * {@link #prepareRegion} applies the camera-relative offset per section.
     */
    public void prepareScene(int viewVersion, Viewport viewport, float searchDistance, int numRegions) {
        Matrix4fc vpMatrix = viewport.getVpMatrix();

        if (vpMatrix == null) {
            throw new IllegalStateException("viewport supplies no view matrix");
        }

        var transform = viewport.getTransform();

        this.backtrackCount = 0;
        this.scene++;

        if (this.regionScene.length < numRegions) {
            int capacity = Math.max(numRegions, this.regionScene.length * 2);
            this.regionScene = Arrays.copyOf(this.regionScene, capacity);
            this.regionTiles = Arrays.copyOf(this.regionTiles, capacity);
            this.regionVersion = Arrays.copyOf(this.regionVersion, capacity);
            this.regionState = Arrays.copyOf(this.regionState, capacity);
            this.regionClip = Arrays.copyOf(this.regionClip, capacity * 3);
        }

        chooseBufferSize(vpMatrix, searchDistance);
        invalidate();
        prepareScene(viewVersion, transform.x, transform.y, transform.z,
                Matrix4L::loadIdentity, m -> copyMatrix(vpMatrix, m));
    }

    /**
     * {@link Matrix4L} indexes {@code a<row><col>} while JOML indexes {@code m<col><row>}, so this
     * transposes. Feeding it straight through instead still produces plausible-looking output.
     */
    private static void copyMatrix(Matrix4fc src, Matrix4L dst) {
        dst.set(
                src.m00(), src.m10(), src.m20(), src.m30(),
                src.m01(), src.m11(), src.m21(), src.m31(),
                src.m02(), src.m12(), src.m22(), src.m32(),
                src.m03(), src.m13(), src.m23(), src.m33());
    }

    /**
     * Sizes each axis of the buffer so that a block at the search distance spans at least one pixel along it,
     * which is what keeps a one-block gap between occluders from closing under pixel-centre sampling. The
     * projection scale of an axis is the length of the matching row of the view-projection matrix: for a
     * perspective matrix those rows are {@code (1/(aspect tan(fov/2)), 0, 0, 0)} and {@code (0, 1/tan(fov/2), 0, 0)},
     * and the camera rotation they are multiplied by preserves length. Bobbing folded into the matrix adds a
     * rotation and a translation, neither of which changes that. The axes differ by the aspect ratio, so a
     * widescreen view needs a wider buffer than it is tall.
     *
     * <p>Sizes are not limited to powers of two, so each axis holds close to what it needs rather than up to
     * double it. An axis only grows when the current buffer falls short and only shrinks when it is more
     * than a step above what is needed, so field-of-view animation cannot thrash it.
     */
    private void chooseBufferSize(Matrix4fc vp, float searchDistance) {
        // orthographic or degenerate matrices have no perspective row; keep whatever we have
        float perspectiveRow = (float) Math.sqrt((vp.m03() * vp.m03()) + (vp.m13() * vp.m13()) + (vp.m23() * vp.m23()));

        if (!(perspectiveRow > 0.5f) || !(searchDistance > 0)) {
            return;
        }

        float scaleX = (float) Math.sqrt((vp.m00() * vp.m00()) + (vp.m10() * vp.m10()) + (vp.m20() * vp.m20()));
        float scaleY = (float) Math.sqrt((vp.m01() * vp.m01()) + (vp.m11() * vp.m11()) + (vp.m21() * vp.m21()));

        if (!(scaleX > 0) || !(scaleY > 0)) {
            return;
        }

        // pixels per block at distance d along an axis is size * scale / (2d)
        float required = 2.0f * searchDistance * PIXELS_PER_BLOCK;
        int widthTiles = chooseAxisTiles(bufferWidth() >> Constants.TILE_AXIS_SHIFT, required / scaleX);
        int heightTiles = chooseAxisTiles(bufferHeight() >> Constants.TILE_AXIS_SHIFT, required / scaleY);
        resizeBuffer(widthTiles, heightTiles);
    }

    /**
     * Grows an axis to the smallest step that meets its requirement, and shrinks it only to one step above
     * the requirement, so the two thresholds never meet: a requirement wavering about a step boundary neither
     * grows nor shrinks the axis, and a sweeping field of view resizes at most once per step.
     */
    private static int chooseAxisTiles(int currentTiles, float requiredPixels) {
        int required = tilesFor(requiredPixels);

        if (required > currentTiles) {
            return required;
        }

        int shrunk = Math.min(Constants.MAX_SIZE_TILES, required + SIZE_STEP_TILES);

        return shrunk < currentTiles ? shrunk : currentTiles;
    }

    /** Tiles needed to hold the given pixels, rounded up to {@link #SIZE_STEP_TILES} and clamped to the buffer limits. */
    private static int tilesFor(float pixels) {
        int tiles = ((int) Math.ceil(pixels / (8 * SIZE_STEP_TILES))) * SIZE_STEP_TILES;

        return Math.max(Constants.MIN_SIZE_TILES, Math.min(Constants.MAX_SIZE_TILES, tiles));
    }

    /** Axis sizes are multiples of this many tiles (64 pixels). */
    private static final int SIZE_STEP_TILES = 8;

    /**
     * Pixels a block at the search distance must span along each axis. Halving this measured within a few
     * percent of the same frame time and culled within a few sections of the same count on the synthetic
     * worlds, so the buffer is not sized for speed: this is what keeps a one-block gap between occluders
     * from closing under pixel-centre sampling.
     */
    private static final float PIXELS_PER_BLOCK = 1.0f;

    /**
     * True if nothing has been drawn where the region projects and the given section's centre is on screen,
     * in which case the section is visible: its centre pixel lies inside the region's screen rectangle, and
     * an untouched pixel on screen is exactly what the centre test looks for. Costs one compare per section
     * once the region's bounds are known, plus a few multiplies when the region is not wholly on screen, and
     * a re-check of a handful of cell rows whenever something new has been drawn since.
     */
    public boolean isSectionUntouched(int regionId, int originX, int originY, int originZ, int chunkX, int chunkY, int chunkZ) {
        final int[] regionVersion = this.regionVersion;

        if (this.regionScene[regionId] != this.scene) {
            this.regionScene[regionId] = this.scene;
            long bounds = boxTileBounds(originX, originY, originZ,
                    RenderRegion.REGION_BLOCK_WIDTH, RenderRegion.REGION_BLOCK_HEIGHT, RenderRegion.REGION_BLOCK_LENGTH);
            this.regionTiles[regionId] = (int) bounds;
            regionVersion[regionId] = REGION_UNVERIFIED;

            if (bounds == AbstractRasterizer.NO_TILE_BOUNDS) {
                this.regionState[regionId] = REGION_NO_BOUNDS;
            } else if ((bounds & AbstractRasterizer.TILE_BOUNDS_ON_SCREEN) != 0) {
                this.regionState[regionId] = REGION_ON_SCREEN;
            } else {
                this.regionState[regionId] = REGION_PARTLY_ON_SCREEN;
                clipPoint(originX + 8, originY + 8, originZ + 8, this.regionClip, regionId * 3);
            }
        }

        final byte state = this.regionState[regionId];

        if (state == REGION_NO_BOUNDS) {
            return false;
        }

        final int version = regionVersion[regionId];
        final int current = touchedVersion();

        if (version != current) {
            if (version == REGION_TOUCHED) {
                return false;
            }

            if (!isTileBoxUntouched(this.regionTiles[regionId])) {
                regionVersion[regionId] = REGION_TOUCHED;
                return false;
            }

            regionVersion[regionId] = current;
        }

        return state == REGION_ON_SCREEN
                || isSectionCenterOnScreen(this.regionClip, regionId * 3,
                        chunkX - (originX >> 4), chunkY - (originY >> 4), chunkZ - (originZ >> 4));
    }

    /**
     * tests the tight bounds of what the section draws first. a section whose geometry is hidden
     * may still have unoccluded empty space, so a second test decides whether traversal continues.
     * must run before occlude(), or a section would occlude itself.
     */
    public SectionVisibility testSection(int originX, int originY, int originZ, int squaredChunkDist, int bounds) {
        if (!AbstractRasterizer.STATS) {
            return testSection0(originX, originY, originZ, squaredChunkDist, bounds);
        }

        long t0 = System.nanoTime();

        try {
            STAT_SECTIONS++;
            var result = testSection0(originX, originY, originZ, squaredChunkDist, bounds);

            if (squaredChunkDist <= NEAR_SQUARED_CHUNK_DIST) {
                STAT_NEAR++;
            } else if (bounds == PackedBox.FULL_BOX) {
                STAT_AIR_TESTS++;
                if (result == SectionVisibility.VISIBLE) STAT_AIR_VISIBLE++;
            }

            return result;
        } finally {
            STAT_TEST_NANOS += System.nanoTime() - t0;
        }
    }

    /** Draws the section's occluder boxes for the region prepared by the last {@link #testSection}. */
    public void occludeSection(int[] data) {
        if (!AbstractRasterizer.STATS) {
            occlude(data);
            return;
        }

        long t0 = System.nanoTime();
        STAT_OCCLUDED_SECTIONS++;
        occlude(data);
        STAT_OCCLUDE_NANOS += System.nanoTime() - t0;
    }

    private SectionVisibility testSection0(int originX, int originY, int originZ, int squaredChunkDist, int bounds) {
        prepareRegion(originX, originY, originZ,
                PackedBox.rangeFromSquareChunkDist(squaredChunkDist), squaredChunkDist);

        if (squaredChunkDist <= NEAR_SQUARED_CHUNK_DIST) {
            return SectionVisibility.VISIBLE;
        }

        if (isBoxCenterClear(bounds)) {
            if (AbstractRasterizer.STATS) STAT_CENTER_HIT++;
            return SectionVisibility.VISIBLE;
        }

        // tests are dilated by a pixel inside the rasterizer, so no world-space fuzz is needed
        if (isBoxVisible(bounds, 0)) {
            return SectionVisibility.VISIBLE;
        }

        if (squaredChunkDist < maxSquaredChunkDistance()) {
            this.backtrackCount++;
        }

        // only worth asking when the bounds were tighter than the section itself
        if (bounds != PackedBox.FULL_BOX && (isBoxCenterClear(PackedBox.FULL_BOX) || isBoxVisible(PackedBox.FULL_BOX, 0))) {
            return SectionVisibility.TRAVERSABLE;
        }

        return SectionVisibility.HIDDEN;
    }

    // the buffer holds coverage only, with no depth, so it's correct only if sections are drawn
    // front to back. our bfs visits in hop-count order, which approximates that but does not
    // guarantee it, so this reports how often the approximation was wrong.
    public int backtrackCount() {
        return this.backtrackCount;
    }
}
