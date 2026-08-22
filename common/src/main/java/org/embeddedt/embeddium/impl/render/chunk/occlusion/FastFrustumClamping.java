package org.embeddedt.embeddium.impl.render.chunk.occlusion;

/*
 * A packed aperture stores the xy, xz and yz angular intervals in six lanes of a single long.
 *
 * Lanes are 10 bits wide and hold a 9-bit rank; the tenth bit is a spare used to allow lanewise addition
 * and subtraction without spilling into neighbours. Lanes 0..2 hold the *complemented*
 * lower ranks (RANK_MASK - lower) and lanes 3..5 the upper ranks, which makes intersection a lane-wise
 * minimum and the hull a lane-wise maximum across all six lanes at once. Both are done with SWAR, so
 * the hot path never unpacks an interval.
 */
final class FastFrustumClamping {
    private static final int TABLE_SHIFT = 5;
    private static final int TABLE_SIZE = 1 << TABLE_SHIFT;
    private static final int RANK_BITS = 9;
    private static final int RANK_MASK = (1 << RANK_BITS) - 1;
    private static final int LANE_BITS = 10;
    private static final int LANES = 6;     // xy, xz, yz lower ranks then their uppers
    private static final int LOW_LANES = 3; // the three lower-rank lanes, used by the emptiness test

    // One bit at each lane origin, LANE_BITS apart.
    private static final long LANE_ONES = laneOnes(LANES);
    private static final long SPARE = LANE_ONES << RANK_BITS;
    private static final long RANKS = LANE_ONES * RANK_MASK;

    // Lanes 0..2 only, used by the emptiness test.
    private static final long LOW_LANE_ONES = laneOnes(LOW_LANES);
    private static final long LOW_SPARE = LOW_LANE_ONES << RANK_BITS;
    private static final long LOW_MASK = (1L << (LOW_LANES * LANE_BITS)) - 1;

    private static final double HALF_PI = Math.PI / 2.0;

    // Entry (x, y) holds the complemented lower rank in lane 0 and the upper rank in lane 3.
    private static final long[] BOUNDS = new long[TABLE_SIZE * TABLE_SIZE];

    static final long EMPTY = -1L;
    static final long FULL = RANKS;

    static {
        for (int x = 0; x < TABLE_SIZE; x++) {
            for (int y = 0; y < TABLE_SIZE; y++) {
                int lower = rankDown(angle(x + 1, y - 1));
                int upper = rankUp(angle(x - 1, y + 1));

                BOUNDS[index(x, y)] = (RANK_MASK - lower) | ((long) upper << (LOW_LANES * LANE_BITS));
            }
        }
    }

    /**
     * How far the camera-relative coordinates must be shifted right to index the table. Shifting all three
     * by the same amount preserves their ratios, which is all the angular bounds depend on.
     *
     * @param maxCoordinate the largest coordinate any {@link #clip} against this cell will use
     */
    static int normalizationShift(int maxCoordinate) {
        return Math.max(0, (Integer.SIZE - Integer.numberOfLeadingZeros(maxCoordinate)) - TABLE_SHIFT);
    }

    /**
     * Intersects each projected interval with the target cell's conservative angular bounds, or returns
     * {@link #EMPTY} once any of the three has closed. Coordinates must already be normalized by
     * {@link #normalizationShift}.
     */
    static long clip(long aperture, int x, int y, int z) {
        // The three table entries land in disjoint lanes, so one OR builds the whole cell bound and one
        // SWAR minimum intersects it with the inherited aperture.
        long bounds = BOUNDS[index(x, y)]
                | (BOUNDS[index(x, z)] << LANE_BITS)
                | (BOUNDS[index(y, z)] << (2 * LANE_BITS));

        long result = min(aperture, bounds);

        return (overlap(result) & LOW_SPARE) == LOW_SPARE ? result : EMPTY;
    }

    // Forms the per-plane hull used when multiple portal paths reach the same cell: the lane-wise maximum
    // over all six lanes. geMask marks the lanes where left >= right, so this keeps the larger operand in
    // each; min() is the same blend with the operands swapped.
    static long hull(long left, long right) {
        long m = geMask(left, right);

        return (left & m) | (right & ~m);
    }

    // The lane-wise minimum over all six lanes, which is the intersection: on the complemented lower lanes
    // a larger stored value is a smaller real lower bound, so this maximizes the lowers while minimizing the
    // uppers. Same blend as hull(), but keeps the smaller operand (the one geMask did not mark).
    private static long min(long left, long right) {
        long m = geMask(left, right);

        return (right & m) | (left & ~m);
    }

    // Returns a mask with a lane's rank bits all set where left >= right and all clear where left < right,
    // computed for all six lanes at once; min() and hull() use it to select an operand per lane.
    private static long geMask(long left, long right) {
        // Pre-set each lane's spare bit, then subtract. The spare bit gives the subtraction a high bit to
        // borrow from, so a lane can never borrow across into its neighbour. That spare bit survives only
        // when the lane did not need to borrow it -- that is, when left >= right -- leaving one flag bit
        // per lane.
        long flags = ((left | SPARE) - right) & SPARE;

        // Broadcast each flag across its whole lane. Shifted down to the lane's low bit it is 0 or 1;
        // subtracting that from the lane's spare bit gives all rank bits set (spare - 1 == RANK_MASK) when
        // the flag was set, and only the spare bit (spare - 0) when it was clear. RANKS drops the spares.
        return (SPARE - (flags >>> RANK_BITS)) & RANKS;
    }

    // Tests all three intervals at once, leaving each lane's spare bit set when that interval overlaps.
    // The lanes stay independent because a lane's arithmetic never carries past its own spare bit.
    private static long overlap(long aperture) {
        long lowers = aperture & LOW_MASK;                               // RANK_MASK - lower, one per lane
        long uppers = (aperture >>> (LOW_LANES * LANE_BITS)) & LOW_MASK; // upper,             one per lane

        // An interval overlaps when lower <= upper. The lane stores RANK_MASK - lower, not lower, so that
        // is the same test as (RANK_MASK - lower) + upper >= RANK_MASK -- an addition we already have.
        // The spare bit sits just above the 9 rank bits, so its value is RANK_MASK + 1; adding one to the
        // sum lifts the >= RANK_MASK threshold to it, so the spare bit fills exactly when lower <= upper.
        return lowers + uppers + LOW_LANE_ONES;
    }

    // One bit at the origin of each of the first `count` lanes, LANE_BITS apart.
    private static long laneOnes(int count) {
        long ones = 0L;
        for (int i = 0; i < count; i++) {
            ones |= 1L << (i * LANE_BITS);
        }
        return ones;
    }

    // Maps a projected cell corner to the first-quadrant angle used by the rank table.
    private static double angle(int run, int rise) {
        return Math.max(0.0, Math.min(HALF_PI, Math.atan2(rise, run)));
    }

    // Rounds a lower angular bound down so rank quantization cannot hide a visible path.
    private static int rankDown(double angle) {
        return Math.max(0, Math.min(RANK_MASK, (int) Math.floor(angle * RANK_MASK / HALF_PI)));
    }

    // Rounds an upper angular bound up so rank quantization cannot hide a visible path.
    private static int rankUp(double angle) {
        return Math.max(0, Math.min(RANK_MASK, (int) Math.ceil(angle * RANK_MASK / HALF_PI)));
    }

    private static int index(int x, int y) {
        return ((x << TABLE_SHIFT) | y) & (TABLE_SIZE * TABLE_SIZE - 1);
    }
}
