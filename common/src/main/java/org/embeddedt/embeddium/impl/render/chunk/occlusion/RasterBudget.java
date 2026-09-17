package org.embeddedt.embeddium.impl.render.chunk.occlusion;

/**
 * Limits how far from the camera the raster tests and draws sections, so its per-frame cost tracks how much it
 * is culling. Sections beyond the limit are treated as visible, which is always conservative.
 *
 * <p>The limit is a squared chunk distance. Each frame the yield of the tests inside it (fraction not visible)
 * feeds a moving average: above {@link #HIGH_YIELD} the limit doubles, below {@link #LOW_YIELD} it halves, in
 * between it holds. It never drops below {@link #FLOOR}, which acts as a standing probe, and stops at the ceiling
 * the search supplies, at which point the search is a full, unbudgeted pass.
 *
 * <p>Limiting by distance rather than by count matters: the search runs roughly front to back, so everything
 * inside the limit is drawn before anything it could hide, and the yield measured there is representative.
 *
 * <p>Representative of the inside, that is: an occluder beyond the limit is never drawn, so whatever it hides is
 * never seen to be hidden, and a cliff a few chunks off would hold the limit at the floor for as long as the
 * camera stood there. So every {@link #PROBE_PERIOD} frames one frame runs unbudgeted, and when that pass pays
 * for itself the limit jumps straight to it.
 *
 * <p>Yield cannot say whether it pays: a hidden section also stops the search behind it, so one cull removes a
 * single section from the frame on gentle hills and dozens on cave-riddled terrain, at a similar yield. The probe
 * instead compares the frame's visible count against the average of the frames before it, which, the camera
 * having barely moved, is what the pass removed, and charges that against the tests it added.
 */
public final class RasterBudget {
    /** Smallest squared chunk distance the limit can take. */
    public static final int FLOOR = 12;

    public static final int HIGH_YIELD = percent(15);
    public static final int LOW_YIELD = percent(5);

    /** Below this many tests in a frame the average is not updated. */
    public static final int MIN_SAMPLE = 16;

    /** The first this many far sections are tested regardless of the limit, so enclosed spaces get a full pass. */
    public static final int CHEAP_PASS = 32;

    /** Every this many frames one runs unbudgeted, to find occlusion the limit cannot reach. */
    public static final int PROBE_PERIOD = 64;

    /** A probe pays when it removes at least one section from the frame for every this many tests it adds. */
    public static final int TESTS_PER_REMOVED = 2;

    private static final int EMA_SHIFT = 2;
    private static final int Q = 16;

    public static final int UNBOUNDED = Integer.MAX_VALUE;

    private int limit = FLOOR;
    private int yieldEma;
    private int ceiling = UNBOUNDED;

    private int frameLimit = FLOOR;

    /** Nothing beyond this is tested this frame; twice the limit's distance, or unbounded on a full pass. */
    private int testable = UNBOUNDED;

    /** Non-zero holds the limit fixed; for benchmarks. */
    private int pinned;

    private int frames;
    private boolean probing;

    /** Moving averages, in 1/16ths, of the visible count and the tests of non-probe frames; -1 before the first. */
    private int visibleEma = -1;
    private int testedEma;

    private int farReached;
    private int farthest;
    private int tested;
    private int culled;

    /** @param ceiling squared chunk distance no reachable section exceeds */
    public void beginFrame(int ceiling) {
        this.ceiling = Math.max(FLOOR, ceiling);
        this.probing = this.pinned == 0 && this.limit < this.ceiling && ++this.frames % PROBE_PERIOD == 0
                && this.visibleEma >= 0;
        this.frameLimit = this.probing ? UNBOUNDED : this.limit();
        this.testable = this.frameLimit >= this.ceiling ? UNBOUNDED
                : Math.min(this.ceiling, saturatingDouble(saturatingDouble(this.frameLimit)));
        this.farReached = 0;
        this.farthest = 0;
        this.tested = 0;
        this.culled = 0;
    }

    /** Called once per far section with occluder data; true if it should be tested and drawn. */
    public boolean shouldTest(int squaredChunkDist) {
        this.farReached++;

        if (squaredChunkDist > this.farthest) {
            this.farthest = squaredChunkDist;
        }

        if (squaredChunkDist > this.testable || (squaredChunkDist > this.frameLimit && this.farReached > CHEAP_PASS)) {
            return false;
        }

        this.tested++;
        return true;
    }

    public void recordCulled() {
        this.culled++;
    }

    /** @param visible sections the search found visible this frame */
    public void endFrame(int visible) {
        if (!this.probing) {
            if (this.visibleEma < 0) {
                this.visibleEma = visible << 4;
                this.testedEma = this.tested << 4;
            } else {
                this.visibleEma += ((visible << 4) - this.visibleEma) >> EMA_SHIFT;
                this.testedEma += ((this.tested << 4) - this.testedEma) >> EMA_SHIFT;
            }
        }

        if (this.tested < MIN_SAMPLE) return;

        int yield = (int) (((long) this.culled << Q) / this.tested);

        if (this.probing) {
            int removed = (this.visibleEma >> 4) - visible;
            int added = this.tested - (this.testedEma >> 4);

            if (removed > 0 && (long) removed * TESTS_PER_REMOVED >= added) {
                this.limit = this.ceiling;
                this.yieldEma = yield;
            }

            return;
        }

        this.yieldEma += (yield - this.yieldEma) >> EMA_SHIFT;

        if (this.pinned != 0) {
            return;
        }

        if (this.yieldEma >= HIGH_YIELD && yield >= HIGH_YIELD) {
            this.limit = Math.min(saturatingDouble(this.limit), this.ceiling);
        } else if (this.yieldEma <= LOW_YIELD) {
            this.limit = Math.max(this.limit >> 1, FLOOR);
        } else {
            this.limit = Math.max(Math.min(this.limit, this.ceiling), FLOOR);
        }
    }

    /** Holds the limit at a fixed value ({@link #UNBOUNDED} for a full pass); zero restores adaptive behaviour. */
    public void pin(int limit) {
        this.pinned = limit;

        if (limit != 0) {
            this.limit = limit;
        }
    }

    public void copyFrom(RasterBudget other) {
        this.limit = other.limit;
        this.yieldEma = other.yieldEma;
        this.visibleEma = other.visibleEma;
        this.testedEma = other.testedEma;
    }

    public int limit() {
        return this.pinned != 0 ? this.pinned : this.limit;
    }

    /** Squared chunk distance beyond which nothing is tested this frame; the coverage buffer is sized to it. */
    public int testableLimit() {
        return this.testable;
    }

    public int farReached() {
        return this.farReached;
    }

    public int tested() {
        return this.tested;
    }

    public int culled() {
        return this.culled;
    }

    /** Moving-average yield in [0, 1]. */
    public float yield() {
        return this.yieldEma / (float) (1 << Q);
    }

    public boolean wasFullPass() {
        return this.tested == this.farReached;
    }

    public boolean wasProbe() {
        return this.probing;
    }

    @Override
    public String toString() {
        return String.format("limit=%s%s yield=%.1f%% far=%d farthest=%d tested=%d culled=%d",
                this.limit() == UNBOUNDED ? "max" : Integer.toString(this.limit()), this.probing ? " (probe)" : "",
                this.yield() * 100.0f, this.farReached, this.farthest, this.tested, this.culled);
    }

    private static int percent(int value) {
        return (int) (((long) value << Q) / 100);
    }

    private static int saturatingDouble(int value) {
        return value >= (1 << 30) ? Integer.MAX_VALUE : value << 1;
    }
}
