package org.embeddedt.embeddium.impl.render.chunk.occlusion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RasterBudgetTest {
    /** Squared chunk distance of the farthest section in the synthetic frames; a 32 chunk render distance. */
    private static final int FARTHEST = 32 * 32;

    /** The bound the culler would hand the controller for such a search. */
    private static final int CEILING = FARTHEST + 1;

    /**
     * Runs one frame over far sections spread evenly in squared distance from just past the near range to
     * {@link #FARTHEST}, where a fraction {@code culledFraction} of admitted tests cull.
     */
    private static void frame(RasterBudget budget, int far, double culledFraction) {
        frame(budget, far, FARTHEST, culledFraction);
    }

    /** As {@link #frame(RasterBudget, int, double)}, with the sections spread only out to {@code farthest}. */
    private static void frame(RasterBudget budget, int far, int farthest, double culledFraction) {
        frame(budget, far, farthest, culledFraction, 1);
    }

    /** As {@link #frame(RasterBudget, int, double)}, where each cull also hides {@code removedPerCull - 1} sections behind it. */
    private static void frame(RasterBudget budget, int far, double culledFraction, int removedPerCull) {
        frame(budget, far, FARTHEST, culledFraction, removedPerCull);
    }

    private static void frame(RasterBudget budget, int far, int farthest, double culledFraction, int removedPerCull) {
        budget.beginFrame(CEILING);
        int admitted = 0;

        for (int i = 0; i < far; i++) {
            int squaredDist = spread(i, far, farthest);

            if (budget.shouldTest(squaredDist)) {
                admitted++;
                if (admitted * culledFraction >= budget.culled() + 1) {
                    budget.recordCulled();
                }
            }
        }

        budget.endFrame(Math.max(0, far - budget.culled() * removedPerCull));
    }

    /** A frame where every admitted test beyond {@code hiddenPast} culls and none nearer does: a cliff at that distance. */
    private static void frameHiddenBeyond(RasterBudget budget, int far, int hiddenPast) {
        budget.beginFrame(CEILING);

        for (int i = 0; i < far; i++) {
            int squaredDist = spread(i, far, FARTHEST);

            if (budget.shouldTest(squaredDist) && squaredDist > hiddenPast) {
                budget.recordCulled();
            }
        }

        budget.endFrame(far - budget.culled());
    }

    /** The {@code i}th of {@code far} squared distances spread evenly from just past the near range to {@code farthest}. */
    private static int spread(int i, int far, int farthest) {
        return RasterOccluder.NEAR_SQUARED_CHUNK_DIST + 1
                + (int) ((long) i * (farthest - RasterOccluder.NEAR_SQUARED_CHUNK_DIST - 1) / Math.max(1, far - 1));
    }

    @Test
    void startsAtFloorAndTestsOnlyTheNearShell() {
        var budget = new RasterBudget();
        frame(budget, 5000, 0.0);
        assertEquals(RasterBudget.FLOOR, budget.limit());
        assertFalse(budget.wasFullPass());
        assertTrue(budget.tested() < 5000 / 10, "tested " + budget.tested());
        assertTrue(budget.tested() >= RasterBudget.MIN_SAMPLE, "tested " + budget.tested());
    }

    @Test
    void lowYieldSettlesAtFloor() {
        var budget = new RasterBudget();

        for (int i = 0; i < 40; i++) {
            frame(budget, 5000, 0.01);
        }

        assertEquals(RasterBudget.FLOOR, budget.limit());
        assertFalse(budget.wasFullPass());
    }

    @Test
    void highYieldRampsToFullPassByDoubling() {
        var budget = new RasterBudget();
        int previous = budget.limit();
        int frames = 0;

        do {
            frame(budget, 4000, 0.9);
            frames++;
            assertTrue(budget.limit() <= previous * 2, "limit grew faster than doubling");
            previous = budget.limit();
        } while (!budget.wasFullPass() && frames < 32);

        assertTrue(budget.wasFullPass(), "never reached a full pass");
        // 12 -> 1025 is seven doublings, plus a couple of frames for the average to cross the threshold
        assertTrue(frames <= 10, "took " + frames + " frames");
        assertEquals(CEILING, budget.limit(), "limit should stop at the ceiling");
    }

    @Test
    void fullPassComesBackDownWhenYieldDrops() {
        var budget = new RasterBudget();

        for (int i = 0; i < 12; i++) {
            frame(budget, 4000, 0.9);
        }
        assertTrue(budget.wasFullPass());

        int frames = 0;

        while (budget.limit() != RasterBudget.FLOOR && frames < 40) {
            frame(budget, 4000, 0.0);
            frames++;
        }

        assertEquals(RasterBudget.FLOOR, budget.limit());
        // the average needs about ten frames to decay below the low threshold, then seven halvings
        assertTrue(frames <= 20, "took " + frames + " frames");
    }

    @Test
    void midYieldHolds() {
        var budget = new RasterBudget();

        // ramp a step, then feed a yield inside the dead band until the average has decayed into it
        frame(budget, 4000, 1.0);

        while (budget.yield() >= 0.15f) {
            frame(budget, 4000, 0.10);
        }

        int held = budget.limit();
        assertTrue(held > RasterBudget.FLOOR);
        assertFalse(budget.wasFullPass());

        for (int i = 0; i < 20; i++) {
            frame(budget, 4000, 0.10);
        }

        assertEquals(held, budget.limit());
    }

    @Test
    void tooSmallASampleLeavesEverythingAlone() {
        var budget = new RasterBudget();
        frame(budget, 4000, 0.9);
        frame(budget, 4000, 0.9);
        int limit = budget.limit();
        float yield = budget.yield();

        // a handful of sections, all within the testable distance
        frame(budget, RasterBudget.MIN_SAMPLE - 1, budget.limit(), 1.0);

        assertTrue(budget.wasFullPass());
        assertEquals(limit, budget.limit());
        assertEquals(yield, budget.yield());
    }

    @Test
    void fewFarSectionsAreTestedInFullWithoutMovingTheLimit() {
        var budget = new RasterBudget();
        // an enclosed space: few sections, none beyond twice the floor's distance
        frame(budget, RasterBudget.CHEAP_PASS, RasterBudget.FLOOR * 4, 0.0);
        assertTrue(budget.wasFullPass());
        assertEquals(RasterBudget.FLOOR, budget.limit());

        // the next frame reaches many more: only the first CHEAP_PASS beyond the limit are admitted
        frame(budget, 4000, 0.0);
        assertFalse(budget.wasFullPass());
        assertTrue(budget.tested() >= RasterBudget.CHEAP_PASS, "tested " + budget.tested());
        assertTrue(budget.tested() < 4000 / 10, "tested " + budget.tested());
    }

    @Test
    void nothingIsTestedBeyondTwiceTheLimitsDistance() {
        var budget = new RasterBudget();
        budget.beginFrame(CEILING);
        assertEquals(RasterBudget.FLOOR * 4, budget.testableLimit());

        // a cheap frame, whose few sections lie beyond the testable distance, still admits none of them
        for (int i = 0; i < RasterBudget.CHEAP_PASS / 2; i++) {
            assertFalse(budget.shouldTest(RasterBudget.FLOOR * 4 + 1));
        }
        assertTrue(budget.shouldTest(RasterBudget.FLOOR * 4));
        budget.endFrame(0);

        budget.pin(RasterBudget.UNBOUNDED);
        budget.beginFrame(CEILING);
        assertEquals(RasterBudget.UNBOUNDED, budget.testableLimit());
    }

    @Test
    void pinnedLimitNeverMovesAndNeverProbes() {
        var budget = new RasterBudget();
        budget.pin(RasterBudget.UNBOUNDED);

        for (int i = 0; i < 20; i++) {
            frame(budget, 4000, 0.0);
            assertTrue(budget.wasFullPass());
        }

        budget.pin(RasterBudget.FLOOR);

        for (int i = 0; i < RasterBudget.PROBE_PERIOD + 2; i++) {
            frame(budget, 4000, 1.0);
            assertEquals(RasterBudget.FLOOR, budget.limit());
            assertFalse(budget.wasFullPass());
            assertFalse(budget.wasProbe());
        }
    }

    @Test
    void probeFindsOcclusionBeyondTheFloorsReach() {
        var budget = new RasterBudget();
        // hidden only past twice the floor's distance, which is as far as the floor ever tests
        int hiddenPast = RasterBudget.FLOOR * 4;

        for (int i = 1; i < RasterBudget.PROBE_PERIOD; i++) {
            frameHiddenBeyond(budget, 4000, hiddenPast);
            assertEquals(RasterBudget.FLOOR, budget.limit(), "frame " + i);
            assertFalse(budget.wasProbe());
        }

        frameHiddenBeyond(budget, 4000, hiddenPast);
        assertTrue(budget.wasProbe());
        assertTrue(budget.wasFullPass());
        assertEquals(CEILING, budget.limit(), "the probe should start a full pass at once");

        // and the full pass measures the same yield, so it stays
        for (int i = 0; i < 8; i++) {
            frameHiddenBeyond(budget, 4000, hiddenPast);
            assertFalse(budget.wasProbe());
            assertTrue(budget.wasFullPass());
            assertEquals(CEILING, budget.limit());
        }
    }

    @Test
    void probeThatFindsNothingChangesNothing() {
        var budget = new RasterBudget();

        for (int i = 1; i < RasterBudget.PROBE_PERIOD; i++) {
            frame(budget, 4000, 0.03);
        }

        int limit = budget.limit();
        float yield = budget.yield();

        frame(budget, 4000, 0.03);
        assertTrue(budget.wasProbe());
        assertTrue(budget.wasFullPass());
        assertEquals(limit, budget.limit());
        assertEquals(yield, budget.yield(), "a probe's yield is not blended into the average");

        frame(budget, 4000, 0.03);
        assertFalse(budget.wasProbe());
        assertFalse(budget.wasFullPass());
    }

    @Test
    void probeFromTheFloorStartsAFullPassThatRemovesEnough() {
        var budget = new RasterBudget();

        // below the yield that doubles the limit, but each cull hides a cave system
        for (int i = 1; i < RasterBudget.PROBE_PERIOD; i++) {
            frame(budget, 4000, 0.1, 20);
            assertEquals(RasterBudget.FLOOR, budget.limit(), "frame " + i);
        }

        frame(budget, 4000, 0.1, 20);
        assertTrue(budget.wasProbe());
        assertEquals(CEILING, budget.limit());
    }

    @Test
    void probeIgnoresAHighYieldThatRemovesTooLittleForItsTests() {
        var budget = new RasterBudget();
        // gentle hills: the far quarter of what the probe tests culls, but each cull hides only itself
        int hiddenPast = (FARTHEST * 3) / 4;

        for (int i = 1; i < RasterBudget.PROBE_PERIOD; i++) {
            frameHiddenBeyond(budget, 4000, hiddenPast);
        }

        frameHiddenBeyond(budget, 4000, hiddenPast);
        assertTrue(budget.wasProbe());
        assertTrue(budget.culled() * 100 >= budget.tested() * 15, "the probe's yield alone would have paid");
        assertEquals(RasterBudget.FLOOR, budget.limit());
    }

    @Test
    void fullPassIsNotProbed() {
        var budget = new RasterBudget();

        for (int i = 0; i < 16; i++) {
            frame(budget, 4000, 0.9);
        }
        assertEquals(CEILING, budget.limit());

        for (int i = 0; i < 2 * RasterBudget.PROBE_PERIOD; i++) {
            frame(budget, 4000, 0.9);
            assertFalse(budget.wasProbe(), "frame " + i);
        }
    }
}
