package org.embeddedt.embeddium.impl.util;

public class PositionUtil {

    private static final long MAX_UNSIGNED_32BIT_INT = 4294967295L;

    public static long packChunk(int x, int z) {
        return (((long)z & MAX_UNSIGNED_32BIT_INT) << 32L) | ((long)x & MAX_UNSIGNED_32BIT_INT);
    }

    public static long packSection(int x, int y, int z) {
        return (((long)x & SECTION_XZ_MASK) << 42L) | (((long)y & SECTION_Y_MASK) << 0L) | (((long)z & SECTION_XZ_MASK) << 20L);
    }

    private static final long SECTION_XZ_MASK = 4194303L;
    private static final long SECTION_Y_MASK = 1048575L;
}
