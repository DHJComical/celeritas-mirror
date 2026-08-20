package org.embeddedt.embeddium.impl.render.chunk.occlusion;

// A packed aperture stores xy, xz, and yz lower/upper ranks in six 10-bit lanes, low to high.
final class FastFrustumClamping {
    private static final int TABLE_SIZE = 64;
    private static final int TARGET_LIMIT = TABLE_SIZE - 2;
    private static final int RANK_BITS = 10;
    private static final int RANK_MASK = (1 << RANK_BITS) - 1;
    private static final double HALF_PI = Math.PI / 2.0;
    private static final long[] BOUNDS = new long[TABLE_SIZE * TABLE_SIZE];

    static final long EMPTY = -1L;
    static final long FULL = pack(0, RANK_MASK, 0, RANK_MASK, 0, RANK_MASK);

    static {
        for (int x = 0; x < TABLE_SIZE; x++) {
            for (int y = 0; y < TABLE_SIZE; y++) {
                BOUNDS[index(x, y)] = pack(
                        rankDown(angle(x + 1, y - 1)),
                        rankUp(angle(x - 1, y + 1)),
                        0, 0, 0, 0
                );
            }
        }
    }

    // Intersects each projected interval with the target cell's conservative angular bounds.
    static long clip(long aperture, int x, int y, int z) {
        while (Math.max(x, Math.max(y, z)) > TARGET_LIMIT) {
            x >>>= 1;
            y >>>= 1;
            z >>>= 1;
        }

        long xy = BOUNDS[index(x, y)];
        long xz = BOUNDS[index(x, z)];
        long yz = BOUNDS[index(y, z)];
        long result = pack(
                Math.max(lower(aperture, 0), lower(xy, 0)), Math.min(upper(aperture, 0), upper(xy, 0)),
                Math.max(lower(aperture, 1), lower(xz, 0)), Math.min(upper(aperture, 1), upper(xz, 0)),
                Math.max(lower(aperture, 2), lower(yz, 0)), Math.min(upper(aperture, 2), upper(yz, 0)));

        return isEmpty(result) ? EMPTY : result;
    }

    // Forms the per-plane hull used when multiple portal paths reach the same cell.
    static long hull(long left, long right) {
        return pack(
                Math.min(lower(left, 0), lower(right, 0)), Math.max(upper(left, 0), upper(right, 0)),
                Math.min(lower(left, 1), lower(right, 1)), Math.max(upper(left, 1), upper(right, 1)),
                Math.min(lower(left, 2), lower(right, 2)), Math.max(upper(left, 2), upper(right, 2))
        );
    }

    // Rejects a path as soon as one of its three projected intervals has no overlap.
    private static boolean isEmpty(long aperture) {
        return lower(aperture, 0) > upper(aperture, 0)
                || lower(aperture, 1) > upper(aperture, 1)
                || lower(aperture, 2) > upper(aperture, 2);
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
        return x * TABLE_SIZE + y;
    }

    // Reads one plane's lower endpoint from the packed aperture.
    private static int lower(long aperture, int plane) {
        return (int) (aperture >>> (plane * 2 * RANK_BITS) & RANK_MASK);
    }

    // Reads one plane's upper endpoint from the packed aperture.
    private static int upper(long aperture, int plane) {
        return (int) (aperture >>> ((plane * 2 + 1) * RANK_BITS) & RANK_MASK);
    }

    // Packs all three projected intervals for hot-path min/max operations.
    private static long pack(int xyLower, int xyUpper, int xzLower, int xzUpper, int yzLower, int yzUpper) {
        return (long) xyLower
                | ((long) xyUpper << RANK_BITS)
                | ((long) xzLower << (2 * RANK_BITS))
                | ((long) xzUpper << (3 * RANK_BITS))
                | ((long) yzLower << (4 * RANK_BITS))
                | ((long) yzUpper << (5 * RANK_BITS));
    }
}
