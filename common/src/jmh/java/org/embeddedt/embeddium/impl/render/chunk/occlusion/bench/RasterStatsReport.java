package org.embeddedt.embeddium.impl.render.chunk.occlusion.bench;

import grondag.bitraster.AbstractRasterizer;
import org.embeddedt.embeddium.impl.render.chunk.RenderSection;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.RasterOccluder;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.SectionLattice;
import org.embeddedt.embeddium.impl.render.viewport.Viewport;

/**
 * Per-frame rasterizer work counters and a test/occlude time split for one world, run with
 * {@code -Dbitraster.stats=true} (the {@code rasterStats} Gradle task sets it). Args: world, and optionally
 * {@code yscan lo hi} to print visible counts (raster/baseline) per yaw step as the camera height varies.
 */
public final class RasterStatsReport {
    private static final int RENDER_DISTANCE = Integer.getInteger("report.renderDistance", 32);
    private static final int YAW_STEPS = 8;
    private static final int WARM = 150;

    public static void main(String[] args) {
        BenchPlatform.ensureInitialized();
        WorldType worldType = args.length > 0 ? WorldType.valueOf(args[0]) : WorldType.SURFACE;

        var world = new SyntheticWorld(worldType, RENDER_DISTANCE, true, true);
        float searchDistance = world.getSearchDistance();
        int numRegions = BenchPlatform.regionManager().getRegionIdsLength();
        var lattice = new SectionLattice(SyntheticWorld.MIN_SECTION_Y, SyntheticWorld.MAX_SECTION_Y, false, true);
        for (RenderSection section : world.getConstructionOrder()) lattice.attach(section);

        double y = Cameras.surfaceCameraY(0, 0);

        if (args.length > 1 && args[1].equals("yscan")) {
            var base = new SectionLattice(SyntheticWorld.MIN_SECTION_Y, SyntheticWorld.MAX_SECTION_Y, false, false);
            for (RenderSection section : world.getConstructionOrder()) base.attach(section);
            int f = 0;
            double lo = args.length > 2 ? Double.parseDouble(args[2]) : -1.0, hi = args.length > 3 ? Double.parseDouble(args[3]) : 3.0;
            for (double dy = lo; dy <= hi; dy += 0.125) {
                StringBuilder sb = new StringBuilder(String.format("y=%7.3f ", y + dy));
                for (int step = 0; step < YAW_STEPS; step++) {
                    Viewport v = Cameras.viewport(8.5, y + dy, 8.5, (360.0f / YAW_STEPS) * step, Cameras.DEFAULT_PITCH, RENDER_DISTANCE);
                    var a = new CountingVisitor(); var b = new CountingVisitor();
                    base.ensureWindowCovers(v.getChunkCoord(), searchDistance);
                    base.findVisible(a, v, searchDistance, numRegions, true, true, ++f);
                    lattice.ensureWindowCovers(v.getChunkCoord(), searchDistance);
                    lattice.findVisible(b, v, searchDistance, numRegions, true, true, ++f);
                    sb.append(String.format("%5d/%5d ", b.visible, a.visible));
                }
                System.out.println(sb);
            }
            return;
        }
        Viewport[] ring = Cameras.yawRing(8.5, y, 8.5, YAW_STEPS, Cameras.DEFAULT_PITCH, RENDER_DISTANCE);
        int frame = 0;

        for (int w = 0; w < WARM; w++) {
            for (Viewport v : ring) {
                lattice.ensureWindowCovers(v.getChunkCoord(), searchDistance);
                lattice.findVisible(new CountingVisitor(), v, searchDistance, numRegions, true, true, ++frame);
            }
        }

        reset();
        long visible = 0;
        long t0 = System.nanoTime();
        for (Viewport v : ring) {
            var c = new CountingVisitor();
            lattice.ensureWindowCovers(v.getChunkCoord(), searchDistance);
            lattice.findVisible(c, v, searchDistance, numRegions, true, true, ++frame);
            visible += c.visible;
        }
        long total = System.nanoTime() - t0;
        int n = YAW_STEPS;
        System.out.printf("world=%s visible/frame=%d total=%.3fms test=%.3fms occlude=%.3fms%n", worldType, visible / n,
                total / 1e6 / n, RasterOccluder.STAT_TEST_NANOS / 1e6 / n, RasterOccluder.STAT_OCCLUDE_NANOS / 1e6 / n);
        System.out.printf("sections tested=%d occluded=%d regionSkip=%d vertexHit=%d%n", RasterOccluder.STAT_SECTIONS / n,
                RasterOccluder.STAT_OCCLUDED_SECTIONS / n, RasterOccluder.STAT_REGION_SKIP / n, AbstractRasterizer.STAT_T_VERTEX / n);
        System.out.printf("draw calls=%d polygons=%d tiles=%d | test calls=%d polygons=%d tiles=%d | eventRows=%d%n",
                AbstractRasterizer.STAT_DRAW_CALLS / n, AbstractRasterizer.STAT_DRAW_QUADS / n,
                (AbstractRasterizer.STAT_D_INNER + AbstractRasterizer.STAT_D_COVER) / n,
                AbstractRasterizer.STAT_TEST_CALLS / n, AbstractRasterizer.STAT_TEST_QUADS / n, AbstractRasterizer.STAT_TEST_TILES / n,
                AbstractRasterizer.STAT_EVENT_ROWS / n);
        printExtra(n);
        System.out.printf("draw noop polygons=%d boxes drawn by range near/mid/far/extreme=%d/%d/%d/%d%n", AbstractRasterizer.STAT_D_NOOP / n,
                AbstractRasterizer.STAT_D_RANGE0 / n, AbstractRasterizer.STAT_D_RANGE1 / n, AbstractRasterizer.STAT_D_RANGE2 / n, AbstractRasterizer.STAT_D_RANGE3 / n);
    }

    private static void printExtra(int n) {
        System.out.printf("centerHit=%d near=%d fullbox tests=%d fullbox visible=%d | test: empty=%d inner=%d cover=%d hidden=%d boxHidden=%d | draw tiles: inner=%d cover=%d%n",
                RasterOccluder.STAT_CENTER_HIT / n, RasterOccluder.STAT_NEAR / n, RasterOccluder.STAT_AIR_TESTS / n, RasterOccluder.STAT_AIR_VISIBLE / n,
                AbstractRasterizer.STAT_T_EMPTY / n, AbstractRasterizer.STAT_T_INNER / n, AbstractRasterizer.STAT_T_COVER / n, AbstractRasterizer.STAT_T_HIDDEN / n, AbstractRasterizer.STAT_T_BOX_HIDDEN / n,
                AbstractRasterizer.STAT_D_INNER / n, AbstractRasterizer.STAT_D_COVER / n);
    }

    private static void reset() {
        AbstractRasterizer.STAT_D_NOOP = AbstractRasterizer.STAT_D_RANGE0 = AbstractRasterizer.STAT_D_RANGE1 = AbstractRasterizer.STAT_D_RANGE2 = AbstractRasterizer.STAT_D_RANGE3 = 0;
        RasterOccluder.STAT_CENTER_HIT = RasterOccluder.STAT_NEAR = RasterOccluder.STAT_AIR_TESTS = RasterOccluder.STAT_AIR_VISIBLE = RasterOccluder.STAT_REGION_SKIP = AbstractRasterizer.STAT_T_VERTEX = 0;
        AbstractRasterizer.STAT_T_EMPTY = AbstractRasterizer.STAT_T_INNER = AbstractRasterizer.STAT_T_COVER = AbstractRasterizer.STAT_T_HIDDEN = AbstractRasterizer.STAT_T_BOX_HIDDEN = 0;
        AbstractRasterizer.STAT_D_INNER = AbstractRasterizer.STAT_D_COVER = 0;
        RasterOccluder.STAT_TEST_NANOS = RasterOccluder.STAT_OCCLUDE_NANOS = RasterOccluder.STAT_SECTIONS = RasterOccluder.STAT_OCCLUDED_SECTIONS = 0;
        AbstractRasterizer.STAT_DRAW_CALLS = AbstractRasterizer.STAT_DRAW_QUADS = 0;
        AbstractRasterizer.STAT_TEST_CALLS = AbstractRasterizer.STAT_TEST_QUADS = AbstractRasterizer.STAT_TEST_TILES = AbstractRasterizer.STAT_EVENT_ROWS = 0;
    }
}
