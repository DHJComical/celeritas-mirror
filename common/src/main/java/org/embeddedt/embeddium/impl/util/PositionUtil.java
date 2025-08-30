package org.embeddedt.embeddium.impl.util;

import org.embeddedt.embeddium.impl.common.util.MathUtil;

public class PositionUtil {

    private static final int SIZE_BITS_X = 26; // range in MC: -30,000,000 to 30,000,000; Range here - [-33554432,
    // 33554431]
    private static final int SIZE_BITS_Z = SIZE_BITS_X; // Same as X
    private static final int SIZE_BITS_Y = 64 - SIZE_BITS_X - SIZE_BITS_Z; // range in MC: [0, 255]; Range here -
    // [-2048, 2047]

    private static final long BITS_X = (1L << SIZE_BITS_X) - 1L;
    private static final long BITS_Y = (1L << SIZE_BITS_Y) - 1L;
    private static final long BITS_Z = (1L << SIZE_BITS_Z) - 1L;

    private static final int BIT_SHIFT_X = SIZE_BITS_Y + SIZE_BITS_Z;
    private static final int BIT_SHIFT_Z = SIZE_BITS_Y;
    private static final int BIT_SHIFT_Y = 0;

    public static long packBlock(int x, int y, int z) {
        long l = 0L;
        l |= ((long) x & BITS_X) << BIT_SHIFT_X;
        l |= ((long) y & BITS_Y) << BIT_SHIFT_Y;
        l |= ((long) z & BITS_Z) << BIT_SHIFT_Z;
        return l;
    }

    public static int unpackBlockX(long packed) {
        return (int) (packed << 64 - BIT_SHIFT_X - SIZE_BITS_X >> 64 - SIZE_BITS_X);
    }

    public static int unpackBlockY(long packed) {
        return (int) (packed << 64 - SIZE_BITS_Y >> 64 - SIZE_BITS_Y);
    }

    public static int unpackBlockZ(long packed) {
        return (int) (packed << 64 - BIT_SHIFT_Z - SIZE_BITS_Z >> 64 - SIZE_BITS_Z);
    }

    private static final long MAX_UNSIGNED_32BIT_INT = 4294967295L;

    public static long packChunk(int x, int z) {
        return (((long)z & MAX_UNSIGNED_32BIT_INT) << 32L) | ((long)x & MAX_UNSIGNED_32BIT_INT);
    }

    public static int unpackChunkX(long key) {
        return (int)(key & MAX_UNSIGNED_32BIT_INT);
    }

    public static int unpackChunkZ(long key) {
        return (int)((key >>> 32) & MAX_UNSIGNED_32BIT_INT);
    }

    public static long packSection(int x, int y, int z) {
        return (((long)x & SECTION_XZ_MASK) << 42L) | (((long)y & SECTION_Y_MASK) << 0L) | (((long)z & SECTION_XZ_MASK) << 20L);
    }

    public static int posToSectionCoord(double coord) {
        return posToSectionCoord(MathUtil.mojfloor(coord));
    }

    public static int posToSectionCoord(int coord) {
        return coord >> 4;
    }

    public static int sectionToBlockCoord(int sec, int block) {
        return (sec << 4) + block;
    }

    private static final long SECTION_XZ_MASK = 4194303L;
    private static final long SECTION_Y_MASK = 1048575L;
}
