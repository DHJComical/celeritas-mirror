package org.embeddedt.embeddium.impl.render.chunk.occlusion.bench;

import org.embeddedt.embeddium.impl.render.chunk.RenderSection;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.SectionLattice;
import org.embeddedt.embeddium.impl.render.viewport.Viewport;

public final class RasterCullReport {
    private static final int RENDER_DISTANCE = Integer.getInteger("report.renderDistance", 32);
    private static final float FOV = Float.parseFloat(System.getProperty("report.fov", "70"));
    private static final int YAW_STEPS = 8;
    private static final WorldType[] WORLDS = { WorldType.SURFACE, WorldType.CAVES };

    private RasterCullReport() {
    }

    private record Placement(String name, double y, float pitch) {
    }

    public static void main(String[] args) {
        BenchPlatform.ensureInitialized();

        if (Boolean.getBoolean("report.fovscan")) {
            fovScan();
            return;
        }

        System.out.printf("%-8s  %-8s  %10s  %10s  %8s  %10s  %10s%n",
                "world", "camera", "baseline", "occluded", "delta", "backtrack", "buffer");
        System.out.println("-".repeat(80));

        for (WorldType world : WORLDS) {
            var syntheticWorld = new SyntheticWorld(world, RENDER_DISTANCE, true, true);
            float searchDistance = syntheticWorld.getSearchDistance();
            int numRegions = BenchPlatform.regionManager().getRegionIdsLength();

            var baselineLattice = new SectionLattice(SyntheticWorld.MIN_SECTION_Y, SyntheticWorld.MAX_SECTION_Y,
                    false, false);
            var rasterLattice = new SectionLattice(SyntheticWorld.MIN_SECTION_Y, SyntheticWorld.MAX_SECTION_Y,
                    false, true);

            for (RenderSection section : syntheticWorld.getConstructionOrder()) {
                baselineLattice.attach(section);
                rasterLattice.attach(section);
            }

            int frame = 0;

            for (Placement placement : placements()) {
                long baseline = 0;
                long occluded = 0;
                long backtracks = 0;

                for (int step = 0; step < YAW_STEPS; step++) {
                    Viewport viewport = Cameras.viewport(8.5, placement.y(), 8.5,
                            (360.0f / YAW_STEPS) * step, placement.pitch(), RENDER_DISTANCE, FOV);

                    var without = new CountingVisitor();
                    baselineLattice.ensureWindowCovers(viewport.getChunkCoord(), searchDistance);
                    baselineLattice.findVisible(without, viewport, searchDistance, numRegions, true, true, ++frame);

                    var with = new CountingVisitor();
                    rasterLattice.ensureWindowCovers(viewport.getChunkCoord(), searchDistance);
                    rasterLattice.findVisible(with, viewport, searchDistance, numRegions, true, true, ++frame);

                    baseline += without.visible;
                    occluded += with.visible;
                    backtracks += rasterLattice.rasterBacktrackCount();
                }

                long meanBaseline = baseline / YAW_STEPS;
                long meanOccluded = occluded / YAW_STEPS;
                double delta = meanBaseline == 0 ? 0.0
                        : ((meanOccluded - meanBaseline) * 100.0) / meanBaseline;

                System.out.printf("%-8s  %-8s  %10d  %10d  %7.1f%%  %10d  %10s%n", world, placement.name(),
                        meanBaseline, meanOccluded, delta, backtracks / YAW_STEPS, rasterLattice.rasterBufferSize());
            }
        }
    }

    /** Sweeps the field of view up and back down and prints the buffer size chosen at each step. */
    private static void fovScan() {
        var world = new SyntheticWorld(WorldType.SURFACE, RENDER_DISTANCE, true, true);
        float searchDistance = world.getSearchDistance();
        int numRegions = BenchPlatform.regionManager().getRegionIdsLength();
        var lattice = new SectionLattice(SyntheticWorld.MIN_SECTION_Y, SyntheticWorld.MAX_SECTION_Y, false, true);

        for (RenderSection section : world.getConstructionOrder()) {
            lattice.attach(section);
        }

        double y = Cameras.surfaceCameraY(0, 0);
        int frame = 0;
        var line = new StringBuilder("rd=" + RENDER_DISTANCE + " fov sweep 30..130..30:");

        int[] fovs = new int[41];

        for (int i = 0; i <= 20; i++) {
            fovs[i] = 30 + (i * 5);
            fovs[40 - i] = fovs[i];
        }

        for (int i = 0; i < fovs.length; i++) {
            int fov = fovs[i];
            Viewport viewport = Cameras.viewport(8.5, y, 8.5, Cameras.DEFAULT_YAW, Cameras.DEFAULT_PITCH, RENDER_DISTANCE, fov);
            lattice.ensureWindowCovers(viewport.getChunkCoord(), searchDistance);
            lattice.findVisible(new CountingVisitor(), viewport, searchDistance, numRegions, true, true, ++frame);
            line.append(' ').append(fov).append(':').append(lattice.rasterBufferSize());

            if (i == 20) {
                line.append(" |");
            }
        }

        System.out.println(line);
    }

    private static Placement[] placements() {
        double surfaceY = Cameras.surfaceCameraY(0, 0);

        return new Placement[] {
                new Placement("surface", surfaceY, Cameras.DEFAULT_PITCH),
                new Placement("treetop", surfaceY + 16.0, Cameras.DEFAULT_PITCH),
                new Placement("high", 200.0, -45.0f),
        };
    }
}
