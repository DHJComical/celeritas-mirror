package org.embeddedt.embeddium.impl.render.chunk.occlusion.bench;

import org.embeddedt.embeddium.impl.render.chunk.data.BuiltRenderSectionData;

/**
 * Built-section data for the synthetic world: visibility bits plus a position-hashed visuals mask (~37% of
 * sections flagged, concentrated in the surface band).
 */
public class BenchSectionData extends BuiltRenderSectionData {
    private static final long VISUALS_SEED = 0x9E37_79B9_7F4A_7C15L;

    private final int visuals;

    public BenchSectionData(long visibilityData, int chunkX, int chunkY, int chunkZ, boolean allFlagged) {
        this.visibilityData = visibilityData;
        this.visuals = visualsOf(chunkX, chunkY, chunkZ, allFlagged);
        this.hasBlockGeometry = (this.visuals & 1) != 0;
    }

    @Override
    public int getVisualBitmaskForSection() {
        return this.visuals;
    }

    /** Position-hashed visuals mask. Surface band (y 1–7): 230/256; elsewhere: 38/256. */
    public static int visualsOf(int chunkX, int chunkY, int chunkZ, boolean allFlagged) {
        long h = chunkX * 0x9E3779B97F4A7C15L ^ chunkY * 0xC2B2AE3D27D4EB4FL ^ chunkZ * 0x165667B19E3779F9L;
        h ^= VISUALS_SEED;
        // splitmix64 finalizer
        h ^= h >>> 30;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 27;
        h *= 0x94D049BB133111EBL;
        h ^= h >>> 31;

        if (!allFlagged) {
            boolean surfaceBand = chunkY >= 1 && chunkY <= 7;
            int roll = (int) (h >>> 40) & 0xFF;

            if (roll >= (surfaceBand ? 230 : 38)) {
                return 0;
            }
        }

        int flags = 1; // HAS_BLOCK_GEOMETRY

        if ((((int) (h >>> 8)) & 0xFF) < 77) {
            flags |= 2; // HAS_SPRITES
        }

        if ((((int) (h >>> 16)) & 0xFF) < 26) {
            flags |= 4; // HAS_BLOCK_ENTITIES
        }

        return flags;
    }
}
