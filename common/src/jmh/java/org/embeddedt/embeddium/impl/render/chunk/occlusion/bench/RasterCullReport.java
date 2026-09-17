package org.embeddedt.embeddium.impl.render.chunk.occlusion.bench;

import org.embeddedt.embeddium.impl.render.chunk.RenderSection;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.RasterBudget;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.SectionLattice;
import org.embeddedt.embeddium.impl.render.viewport.Viewport;

public final class RasterCullReport {
    private static final int RENDER_DISTANCE = Integer.getInteger("report.renderDistance", 32);
    private static final float FOV = Float.parseFloat(System.getProperty("report.fov", "70"));
    private static final int YAW_STEPS = 8;
    /** Frames the adaptive budget is given to settle before a placement is measured; covers a probe. */
    private static final int WARM_FRAMES = RasterBudget.PROBE_PERIOD + 32;
    private static final int TIMED_ROUNDS = 5;
    private static final WorldType[] WORLDS = worlds();

    private static WorldType[] worlds() {
        String list = System.getProperty("report.worlds", "PLAINS,HILLS,CLIFF,FLAT,SURFACE,CAVES");
        return java.util.Arrays.stream(list.split(",")).map(String::trim).map(WorldType::valueOf).toArray(WorldType[]::new);
    }

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

        if (Boolean.getBoolean("report.transition")) {
            transitionReport();
            return;
        }

        System.out.printf("%-8s  %-8s  %10s  %10s  %8s  %10s  %8s  %6s  %6s  %7s  %7s  %7s  %9s  %10s%n",
                "world", "camera", "baseline", "occluded", "delta", "budgeted", "bdelta", "limit", "yield",
                "ms:base", "raster", "budget", "backtrack", "buffer", "bbuffer");
        System.out.println("-".repeat(150));

        for (WorldType world : WORLDS) {
            var syntheticWorld = new SyntheticWorld(world, RENDER_DISTANCE, true, true);
            float searchDistance = syntheticWorld.getSearchDistance();
            int numRegions = BenchPlatform.regionManager().getRegionIdsLength();

            var baselineLattice = new SectionLattice(SyntheticWorld.MIN_SECTION_Y, SyntheticWorld.MAX_SECTION_Y,
                    false, false);
            var rasterLattice = new SectionLattice(SyntheticWorld.MIN_SECTION_Y, SyntheticWorld.MAX_SECTION_Y,
                    false, true);
            var budgetedLattice = new SectionLattice(SyntheticWorld.MIN_SECTION_Y, SyntheticWorld.MAX_SECTION_Y,
                    false, true);

            for (RenderSection section : syntheticWorld.getConstructionOrder()) {
                baselineLattice.attach(section);
                rasterLattice.attach(section);
                budgetedLattice.attach(section);
            }

            BudgetMode.MAX.apply(rasterLattice.rasterBudget());
            BudgetMode.ADAPTIVE.apply(budgetedLattice.rasterBudget());

            int frame = 0;

            for (Placement placement : placements()) {
                Viewport[] ring = new Viewport[YAW_STEPS];

                for (int step = 0; step < YAW_STEPS; step++) {
                    ring[step] = Cameras.viewport(8.5, placement.y(), 8.5,
                            (360.0f / YAW_STEPS) * step, placement.pitch(), RENDER_DISTANCE, FOV);
                }

                // Let the controller settle on this placement, and the JIT on all three lattices, before measuring.
                for (int warm = 0; warm < WARM_FRAMES; warm++) {
                    Viewport viewport = ring[warm % YAW_STEPS];
                    frame = runRing(baselineLattice, new Viewport[] { viewport }, searchDistance, numRegions, frame, null);
                    frame = runRing(rasterLattice, new Viewport[] { viewport }, searchDistance, numRegions, frame, null);
                    frame = runRing(budgetedLattice, new Viewport[] { viewport }, searchDistance, numRegions, frame, null);
                }

                // Fastest of several rounds, so a GC or a scheduling hiccup does not stand in for the cost.
                long[] visible = new long[3];
                long[] nanos = { Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE };
                SectionLattice[] lattices = { baselineLattice, rasterLattice, budgetedLattice };

                for (int round = 0; round < TIMED_ROUNDS; round++) {
                    for (int i = 0; i < lattices.length; i++) {
                        long[] count = new long[1];
                        long t0 = System.nanoTime();
                        frame = runRing(lattices[i], ring, searchDistance, numRegions, frame, count);
                        nanos[i] = Math.min(nanos[i], System.nanoTime() - t0);
                        visible[i] = count[0];
                    }
                }

                long backtracks = 0;

                for (Viewport viewport : ring) {
                    rasterLattice.ensureWindowCovers(viewport.getChunkCoord(), searchDistance);
                    rasterLattice.findVisible(new CountingVisitor(), viewport, searchDistance, numRegions, true, true, ++frame);
                    backtracks += rasterLattice.rasterBacktrackCount();
                }

                long meanBaseline = visible[0] / YAW_STEPS;
                long meanOccluded = visible[1] / YAW_STEPS;
                long meanBudgeted = visible[2] / YAW_STEPS;
                var budget = budgetedLattice.rasterBudget();

                System.out.printf("%-8s  %-8s  %10d  %10d  %7.1f%%  %10d  %7.1f%%  %6s  %5.1f%%  %7.3f  %7.3f  %7.3f  %9d  %10s  %10s%n", world, placement.name(),
                        meanBaseline, meanOccluded, percentDelta(meanBaseline, meanOccluded),
                        meanBudgeted, percentDelta(meanBaseline, meanBudgeted),
                        describe(budget), budget.yield() * 100.0f,
                        nanos[0] / 1e6 / YAW_STEPS, nanos[1] / 1e6 / YAW_STEPS, nanos[2] / 1e6 / YAW_STEPS,
                        backtracks / YAW_STEPS, rasterLattice.rasterBufferSize(), budgetedLattice.rasterBufferSize());
            }

            rampReport(world, budgetedLattice, searchDistance, numRegions, frame);
        }
    }

    /** Searches every viewport of the ring once, summing visible counts into {@code visibleOut[0]} when given. */
    private static int runRing(SectionLattice lattice, Viewport[] ring, float searchDistance, int numRegions, int frame, long[] visibleOut) {
        for (Viewport viewport : ring) {
            var visitor = new CountingVisitor();
            lattice.ensureWindowCovers(viewport.getChunkCoord(), searchDistance);
            lattice.findVisible(visitor, viewport, searchDistance, numRegions, true, true, ++frame);

            if (visibleOut != null) {
                visibleOut[0] += visitor.visible;
            }
        }

        return frame;
    }

    private static double percentDelta(long baseline, long value) {
        return baseline == 0 ? 0.0 : ((value - baseline) * 100.0) / baseline;
    }

    private static String describe(RasterBudget budget) {
        return budget.wasProbe() ? "probe" : budget.wasFullPass() ? "full" : Integer.toString(budget.limit());
    }

    /**
     * Settles the controller, from the floor, on the placement with the least occlusion, then jumps to the one with
     * the most and counts frames until the budget stops changing, over two probe periods. This is the latency a
     * player sees walking into a cave. The trace lists each frame at which the budget's state changed.
     */
    private static void rampReport(WorldType world, SectionLattice lattice, float searchDistance, int numRegions, int frame) {
        Placement[] placements = placements();
        Placement from = placements[placements.length - 1];
        Placement to = placements[0];
        var budget = lattice.rasterBudget();
        budget.copyFrom(new RasterBudget());

        for (int warm = 0; warm < WARM_FRAMES; warm++) {
            Viewport viewport = Cameras.viewport(8.5, from.y(), 8.5, (360.0f / YAW_STEPS) * (warm % YAW_STEPS),
                    from.pitch(), RENDER_DISTANCE, FOV);
            lattice.ensureWindowCovers(viewport.getChunkCoord(), searchDistance);
            lattice.findVisible(new CountingVisitor(), viewport, searchDistance, numRegions, true, true, ++frame);
        }

        int startBudget = budget.limit();
        var trace = new StringBuilder();
        int settledAt = -1;
        int previous = startBudget;
        String previousState = null;

        for (int i = 0; i < 2 * RasterBudget.PROBE_PERIOD; i++) {
            Viewport viewport = Cameras.viewport(8.5, to.y(), 8.5, (360.0f / YAW_STEPS) * (i % YAW_STEPS),
                    to.pitch(), RENDER_DISTANCE, FOV);
            lattice.ensureWindowCovers(viewport.getChunkCoord(), searchDistance);
            lattice.findVisible(new CountingVisitor(), viewport, searchDistance, numRegions, true, true, ++frame);

            String state = describe(budget);

            if (!state.equals(previousState)) {
                trace.append(' ').append(i).append(':').append(state);
            }

            if (budget.limit() != previous) {
                settledAt = -1;
            } else if (settledAt < 0) {
                settledAt = i;
            }

            previous = budget.limit();
            previousState = state;
        }

        System.out.printf("%-8s  ramp %s -> %s: limit %d -> %s, settled after %d frames, trace:%s%n", world, from.name(), to.name(),
                startBudget, describe(budget), settledAt, trace);
    }

    /**
     * Carries a controller settled on open ground (PLAINS, where the raster culls nothing) into terrain with caves
     * beneath (FLAT, where it culls a quarter of the sections), and back, and prints every frame of both ramps: the
     * limit, what the adaptive search reports visible against the unbudgeted raster and no raster at all, and the
     * time of each. This is what a player sees walking from a field into a forest with caves, and it is where a
     * count-based ration could produce a single expensive frame.
     */
    private static void transitionReport() {
        int numRegions;
        var plains = new SyntheticWorld(WorldType.PLAINS, RENDER_DISTANCE, true, true);
        var flat = new SyntheticWorld(WorldType.FLAT, RENDER_DISTANCE, true, true);
        numRegions = BenchPlatform.regionManager().getRegionIdsLength();
        float searchDistance = plains.getSearchDistance();

        SectionLattice[][] lattices = new SectionLattice[2][3];
        SyntheticWorld[] worlds = { plains, flat };

        for (int w = 0; w < 2; w++) {
            for (int i = 0; i < 3; i++) {
                lattices[w][i] = new SectionLattice(SyntheticWorld.MIN_SECTION_Y, SyntheticWorld.MAX_SECTION_Y, false, i != 0);

                for (RenderSection section : worlds[w].getConstructionOrder()) {
                    lattices[w][i].attach(section);
                }
            }

            BudgetMode.MAX.apply(lattices[w][1].rasterBudget());
            BudgetMode.ADAPTIVE.apply(lattices[w][2].rasterBudget());
        }

        double y = Cameras.surfaceCameraY(0, 0);
        Viewport[] ring = Cameras.yawRing(8.5, y, 8.5, YAW_STEPS, Cameras.DEFAULT_PITCH, RENDER_DISTANCE);
        int frame = 0;

        // Warm the JIT on everything, and settle both adaptive controllers on their own world.
        for (int warm = 0; warm < WARM_FRAMES * 2; warm++) {
            for (SectionLattice[] set : lattices) {
                for (SectionLattice lattice : set) {
                    frame = runRing(lattice, new Viewport[] { ring[warm % YAW_STEPS] }, searchDistance, numRegions, frame, null);
                }
            }
        }

        frame = transition("PLAINS -> FLAT", lattices[0][2], lattices[1], ring, searchDistance, numRegions, frame);
        // the FLAT controller is now at whatever the ramp left it, which is the state a player carries back out
        transition("FLAT -> PLAINS", lattices[1][2], lattices[0], ring, searchDistance, numRegions, frame);
    }

    private static int transition(String name, SectionLattice from, SectionLattice[] to, Viewport[] ring,
                                  float searchDistance, int numRegions, int frame) {
        var budget = to[2].rasterBudget();
        budget.copyFrom(from.rasterBudget());

        System.out.printf("%n%s (rd=%d), controller arrives with limit=%d yield=%.1f%%%n", name, RENDER_DISTANCE,
                budget.limit(), budget.yield() * 100.0f);
        System.out.printf("%5s  %6s  %6s  %9s  %9s  %9s  %8s  %8s  %8s%n",
                "frame", "limit", "yield", "vis:base", "raster", "adaptive", "ms:base", "raster", "adaptive");

        for (int i = 0; i < 24; i++) {
            Viewport viewport = ring[i % YAW_STEPS];
            long[] visible = new long[3];
            double[] ms = new double[3];

            for (int l = 0; l < 3; l++) {
                var visitor = new CountingVisitor();
                to[l].ensureWindowCovers(viewport.getChunkCoord(), searchDistance);
                long t0 = System.nanoTime();
                to[l].findVisible(visitor, viewport, searchDistance, numRegions, true, true, ++frame);
                ms[l] = (System.nanoTime() - t0) / 1e6;
                visible[l] = visitor.visible;
            }

            System.out.printf("%5d  %6s  %5.1f%%  %9d  %9d  %9d  %8.3f  %8.3f  %8.3f%n", i,
                    describe(budget), budget.yield() * 100.0f,
                    visible[0], visible[1], visible[2], ms[0], ms[1], ms[2]);
        }

        return frame;
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

        // Ordered from most to least occluded: the ramp report jumps from the last to the first.
        return new Placement[] {
                new Placement("surface", surfaceY, Cameras.DEFAULT_PITCH),
                new Placement("treetop", surfaceY + 16.0, Cameras.DEFAULT_PITCH),
                // looking up: nothing in view has occluder data, so the raster tests nothing
                new Placement("sky", 200.0, -45.0f),
                // looking down on the terrain from far above: everything is a shell, nothing occludes anything
                new Placement("aerial", surfaceY + 96.0, 45.0f),
        };
    }
}
