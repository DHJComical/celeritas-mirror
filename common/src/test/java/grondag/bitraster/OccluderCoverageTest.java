package grondag.bitraster;

import org.embeddedt.embeddium.impl.render.chunk.occlusion.RasterOccluder;
import org.embeddedt.embeddium.impl.render.viewport.Viewport;
import org.embeddedt.embeddium.impl.render.viewport.frustum.SimpleFrustum;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Checks the rasterizer against the exact projected silhouettes of single boxes seen from random cameras.
 *
 * <p>Drawing and testing are held to pixel centres from opposite sides. A drawn occluder must set no pixel whose
 * centre is outside its silhouette, since the test dilation only reaches one pixel past what it tests and any
 * such pixel can close a visible gap between two occluders. A tested box must find any clear pixel whose centre
 * is inside its silhouette, or a section showing through that pixel is culled.
 */
class OccluderCoverageTest {
    /** Slack for the 1/16 pixel vertex snapping and float projection. */
    private static final double TOLERANCE = 0.07;

    private static final int TRIALS = 400;

    /** A random box some distance in front of a random camera, with its projected silhouette in buffer pixels. */
    private record Scene(Viewport viewport, int originX, int originY, int originZ, int box, double[][] hull,
                         float yaw, float pitch) {
        RasterOccluder occluder() {
            RasterOccluder occluder = new RasterOccluder();
            occluder.prepareScene(1, viewport, 128.0f);
            return occluder;
        }

        String describe() {
            return String.format("yaw=%.1f pitch=%.1f box %s at %d,%d,%d", yaw, pitch, PackedBox.toString(box), originX, originY, originZ);
        }
    }

    /** Returns null if the box is too close to the camera to project without near clipping. */
    private static Scene randomScene(Random random, int trial, int[] lo, int[] hi) {
        // every fourth view is level, so box edges that are vertical in the world stay vertical on screen
        float yaw = random.nextFloat() * 360.0f;
        float pitch = trial % 4 == 0 ? 0.0f : (random.nextFloat() - 0.5f) * 120.0f;
        Matrix4f rotation = new Matrix4f().rotateX((float) Math.toRadians(pitch)).rotateY((float) Math.toRadians(yaw));
        Matrix4f matrix = new Matrix4f().perspective((float) Math.toRadians(70.0), 16.0f / 9.0f, 0.05f, 512.0f).mul(rotation);
        Vector3d camera = new Vector3d(random.nextDouble() * 16, random.nextDouble() * 16, random.nextDouble() * 16);
        Viewport viewport = new Viewport(new SimpleFrustum(new FrustumIntersection(matrix)), camera, matrix);

        // the section some distance straight ahead
        Vector4f forward = new Matrix4f(rotation).invert().transform(new Vector4f(0, 0, -1, 0));
        double distance = 24 + (random.nextDouble() * 80);
        int originX = (int) Math.floor((camera.x + (forward.x * distance)) / 16) * 16;
        int originY = (int) Math.floor((camera.y + (forward.y * distance)) / 16) * 16;
        int originZ = (int) Math.floor((camera.z + (forward.z * distance)) / 16) * 16;
        int box = PackedBox.pack(lo[0], lo[1], lo[2], hi[0], hi[1], hi[2], PackedBox.RANGE_EXTREME);

        RasterOccluder sizing = new RasterOccluder();
        sizing.prepareScene(1, viewport, 128.0f);
        int width = sizing.bufferWidth(), height = sizing.bufferHeight();
        double[][] corners = new double[8][];

        for (int c = 0; c < 8; c++) {
            Vector4f v = matrix.transform(new Vector4f(
                    (float) (originX + ((c & 1) != 0 ? hi[0] : lo[0]) - camera.x),
                    (float) (originY + ((c & 2) != 0 ? hi[1] : lo[1]) - camera.y),
                    (float) (originZ + ((c & 4) != 0 ? hi[2] : lo[2]) - camera.z), 1.0f));

            if (v.w <= 0.1f) {
                return null;
            }

            corners[c] = new double[] { ((v.x / v.w) + 1) * width / 2.0, ((v.y / v.w) + 1) * height / 2.0 };
        }

        return new Scene(viewport, originX, originY, originZ, box, convexHull(corners), yaw, pitch);
    }

    private static Scene randomScene(Random random, int trial) {
        int[] lo = { random.nextInt(15), random.nextInt(15), random.nextInt(15) };
        int[] hi = new int[3];

        for (int a = 0; a < 3; a++) {
            hi[a] = lo[a] + 1 + random.nextInt(16 - lo[a]);
        }

        return randomScene(random, trial, lo, hi);
    }

    @Test
    void occludersCoverNoPixelCentreOutsideTheirSilhouette() {
        Random random = new Random(1234);
        long over = 0, pixels = 0;
        double worst = 0;
        String example = null;

        for (int trial = 0; trial < TRIALS; trial++) {
            Scene scene = randomScene(random, trial);

            if (scene == null) {
                continue;
            }

            RasterOccluder occluder = scene.occluder();
            occluder.occludeSectionAt(scene.originX, scene.originY, scene.originZ, new int[] { scene.box, scene.box });
            AbstractRasterizer raster = occluder.raster;
            int[] bounds = pixelBounds(scene.hull, raster);

            for (int y = bounds[1]; y <= bounds[3]; y++) {
                for (int x = bounds[0]; x <= bounds[2]; x++) {
                    pixels++;
                    double outside = distanceOutside(scene.hull, x + 0.5, y + 0.5);

                    if (outside > TOLERANCE && raster.isPixelSet(x, y)) {
                        over++;

                        if (outside > worst) {
                            worst = outside;
                            example = String.format("%s pixel=(%d,%d) centre %.3f px outside", scene.describe(), x, y, outside);
                        }
                    }
                }
            }
        }

        assertEquals(0, over, over + " of " + pixels + " pixels set with their centre outside the silhouette; worst " + example);
    }

    /**
     * Covers the whole buffer but one pixel whose centre is inside the box's silhouette; the box must be found
     * visible through it. Pixels just inside the silhouette's edge are the ones a misplaced span end loses.
     */
    @Test
    void testedBoxesFindEveryClearPixelCentreInside() {
        Random random = new Random(4321);
        long missed = 0, checks = 0;
        String example = null;

        for (int trial = 0; trial < TRIALS; trial++) {
            Scene scene = randomScene(random, trial);

            if (scene == null) {
                continue;
            }

            RasterOccluder occluder = scene.occluder();
            AbstractRasterizer raster = occluder.raster;
            int[] bounds = pixelBounds(scene.hull, raster);

            for (int y = bounds[1]; y <= bounds[3]; y++) {
                for (int x = bounds[0]; x <= bounds[2]; x++) {
                    double outside = distanceOutside(scene.hull, x + 0.5, y + 0.5);

                    // only centres just inside the edge, where a misplaced span end shows
                    if (outside > -TOLERANCE || outside < -1.0) {
                        continue;
                    }

                    checks++;
                    Arrays.fill(raster.tiles, -1L);
                    raster.tiles[raster.tileIndexFromPixelXY(x, y)] &= ~(1L << Indexer.pixelIndex(x, y));
                    occluder.prepareRegion(scene.originX, scene.originY, scene.originZ, PackedBox.RANGE_EXTREME, 64);

                    if (!occluder.isBoxVisible(scene.box, 0)) {
                        missed++;

                        if (example == null) {
                            example = String.format("%s pixel=(%d,%d) centre %.3f px inside", scene.describe(), x, y, -outside);
                        }
                    }
                }
            }
        }

        assertEquals(0, missed, missed + " of " + checks + " clear pixels inside a silhouette were not found; first " + example);
    }

    /**
     * A box drawn as two abutting halves must cover every pixel centre inside the whole box: a centre on the shared
     * edge has to land in one half or the other, or every seam between occluder boxes would leak.
     */
    @Test
    void abuttingOccludersLeaveNoSeam() {
        Random random = new Random(5678);
        long seams = 0, pixels = 0;
        String example = null;

        for (int trial = 0; trial < TRIALS; trial++) {
            int[] lo = { random.nextInt(14), random.nextInt(14), random.nextInt(14) };
            int[] hi = new int[3];

            for (int a = 0; a < 3; a++) {
                hi[a] = lo[a] + 2 + random.nextInt(15 - lo[a]);
            }

            Scene scene = randomScene(random, trial, lo, hi);

            if (scene == null) {
                continue;
            }

            int axis = random.nextInt(3);
            int split = lo[axis] + 1 + random.nextInt(hi[axis] - lo[axis] - 1);
            int[] firstHi = hi.clone(), secondLo = lo.clone();
            firstHi[axis] = split;
            secondLo[axis] = split;
            int first = PackedBox.pack(lo[0], lo[1], lo[2], firstHi[0], firstHi[1], firstHi[2], PackedBox.RANGE_EXTREME);
            int second = PackedBox.pack(secondLo[0], secondLo[1], secondLo[2], hi[0], hi[1], hi[2], PackedBox.RANGE_EXTREME);

            RasterOccluder whole = scene.occluder();
            whole.occludeSectionAt(scene.originX, scene.originY, scene.originZ, new int[] { scene.box, scene.box });
            RasterOccluder halves = scene.occluder();
            halves.occludeSectionAt(scene.originX, scene.originY, scene.originZ, new int[] { scene.box, first, second });

            AbstractRasterizer wholeRaster = whole.raster, halvesRaster = halves.raster;

            for (int y = 0; y < wholeRaster.pixelHeight; y++) {
                for (int x = 0; x < wholeRaster.pixelWidth; x++) {
                    if (wholeRaster.isPixelSet(x, y)) {
                        pixels++;

                        // a centre within the tolerance of the outer silhouette is a near tie that the halves may
                        // round the other way, since splitting adds a snapped vertex on that outer edge
                        if (!halvesRaster.isPixelSet(x, y) && distanceOutside(scene.hull, x + 0.5, y + 0.5) < -TOLERANCE) {
                            seams++;

                            if (example == null) {
                                example = String.format("%s split axis %d at %d, pixel=(%d,%d)", scene.describe(), axis, split, x, y);
                            }
                        }
                    }
                }
            }
        }

        assertEquals(0, seams, seams + " of " + pixels + " pixels covered by a whole box are missed by its halves; first " + example);
    }

    /** Pixel rectangle {minX, minY, maxX, maxY} around a silhouette, with a two pixel margin, within the buffer. */
    private static int[] pixelBounds(double[][] hull, AbstractRasterizer raster) {
        double minX = Arrays.stream(hull).mapToDouble(p -> p[0]).min().orElseThrow();
        double maxX = Arrays.stream(hull).mapToDouble(p -> p[0]).max().orElseThrow();
        double minY = Arrays.stream(hull).mapToDouble(p -> p[1]).min().orElseThrow();
        double maxY = Arrays.stream(hull).mapToDouble(p -> p[1]).max().orElseThrow();

        return new int[] {
                Math.max(0, (int) minX - 2), Math.max(0, (int) minY - 2),
                Math.min(raster.pixelWidth - 1, (int) maxX + 2), Math.min(raster.pixelHeight - 1, (int) maxY + 2)
        };
    }

    /** Distance in pixels from a point to a counter-clockwise convex polygon, positive outside and negative inside. */
    private static double distanceOutside(double[][] hull, double px, double py) {
        double worst = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < hull.length; i++) {
            double[] a = hull[i], b = hull[(i + 1) % hull.length];
            double ex = b[0] - a[0], ey = b[1] - a[1];
            worst = Math.max(worst, -((ex * (py - a[1])) - (ey * (px - a[0]))) / Math.hypot(ex, ey));
        }

        return worst;
    }

    /** Andrew's monotone chain; counter-clockwise, without collinear points. */
    private static double[][] convexHull(double[][] points) {
        double[][] sorted = points.clone();
        Arrays.sort(sorted, (p, q) -> p[0] != q[0] ? Double.compare(p[0], q[0]) : Double.compare(p[1], q[1]));
        double[][] hull = new double[2 * sorted.length][];
        int k = 0;

        for (double[] p : sorted) {
            while (k >= 2 && cross(hull[k - 2], hull[k - 1], p) <= 0) k--;
            hull[k++] = p;
        }

        for (int i = sorted.length - 2, lower = k + 1; i >= 0; i--) {
            while (k >= lower && cross(hull[k - 2], hull[k - 1], sorted[i]) <= 0) k--;
            hull[k++] = sorted[i];
        }

        return Arrays.copyOf(hull, k - 1);
    }

    private static double cross(double[] o, double[] a, double[] b) {
        return ((a[0] - o[0]) * (b[1] - o[1])) - ((a[1] - o[1]) * (b[0] - o[0]));
    }
}
