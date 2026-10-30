package org.embeddedt.embeddium.impl.render.chunk.terrain.sampling;

import org.junit.jupiter.api.Test;

import static org.embeddedt.embeddium.impl.render.chunk.terrain.sampling.AtlasEdgeMask.*;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class AtlasEdgeMaskTest {
    private static final int ALL_BORDERS = BORDER_LEFT | BORDER_RIGHT | BORDER_TOP | BORDER_BOTTOM;

    /** A 64x32 atlas with 4 mip levels, so 4x2 cells of 16 texels. */
    private static byte[] flags(int... spriteRects) {
        return AtlasEdgeMask.computeCellFlags(4, 2, 4, spriteRects);
    }

    private static byte[] expected(int... flags) {
        byte[] bytes = new byte[flags.length];
        for (int i = 0; i < flags.length; i++) {
            bytes[i] = (byte) flags[i];
        }
        return bytes;
    }

    @Test
    void bordersOnlyBetweenSprites() {
        assertArrayEquals(expected(
                BORDER_LEFT | BORDER_TOP | BORDER_BOTTOM, BORDER_RIGHT | BORDER_TOP | BORDER_BOTTOM, ALL_BORDERS, FALLBACK,
                FALLBACK, FALLBACK, FALLBACK, FALLBACK
        ), flags(0, 0, 32, 16, 32, 0, 16, 16));
    }

    @Test
    void spriteSpanningRowsHasNoInternalBorder() {
        assertArrayEquals(expected(
                BORDER_LEFT | BORDER_RIGHT | BORDER_TOP, FALLBACK, FALLBACK, FALLBACK,
                BORDER_LEFT | BORDER_RIGHT | BORDER_BOTTOM, FALLBACK, FALLBACK, FALLBACK
        ), flags(0, 0, 16, 32));
    }

    @Test
    void partiallyCoveredCellFallsBack() {
        // A 24px sprite, as Forge allows without capping the mip level, ends halfway through its second cell.
        assertArrayEquals(expected(
                ALL_BORDERS, FALLBACK, FALLBACK, FALLBACK,
                FALLBACK, FALLBACK, FALLBACK, FALLBACK
        ), flags(0, 0, 24, 16));
    }

    @Test
    void unalignedSpriteFallsBack() {
        assertArrayEquals(expected(
                FALLBACK, FALLBACK, ALL_BORDERS, FALLBACK,
                FALLBACK, FALLBACK, FALLBACK, FALLBACK
        ), flags(8, 0, 16, 16, 32, 0, 16, 16));
    }

    @Test
    void sharedCellFallsBack() {
        assertArrayEquals(expected(
                FALLBACK, FALLBACK, FALLBACK, FALLBACK,
                FALLBACK, FALLBACK, FALLBACK, FALLBACK
        ), flags(0, 0, 16, 16, 0, 0, 16, 16));
    }
}
