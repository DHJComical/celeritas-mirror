package org.embeddedt.embeddium.impl.render.chunk.bench.voxel;

import org.embeddedt.embeddium.impl.render.chunk.occlusion.SectionVisibilityBuilder;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.VisibilityEncoding;

/**
 * One section of a {@link VoxelWorld} plus a one-block border, materialised into a dense array.
 * Mirrors {@code ChunkCache}/{@code WorldSlice}.
 */
public final class VoxelSlice {
    static final int SIZE = 18; // 16 + one-block border on each side
    static final int AREA = SIZE * SIZE;

    private final byte[] blocks;
    private final boolean empty;

    VoxelSlice(VoxelWorld world, int chunkX, int chunkY, int chunkZ) {
        if (world.isVoid()) {
            this.blocks = null;
            this.empty = true;
            return;
        }

        final int baseX = (chunkX << 4) - 1;
        final int baseY = (chunkY << 4) - 1;
        final int baseZ = (chunkZ << 4) - 1;

        // Column heights are shared by every section in the column, so they are computed once and cached there.
        int[] heights = world.columnHeights(chunkX, chunkZ);
        int maxHeight = heights[AREA];

        // Skip sections that are entirely air above both terrain and sea level.
        if (baseY >= maxHeight && baseY >= VoxelWorld.SEA_LEVEL) {
            this.blocks = null;
            this.empty = true;
            return;
        }

        // Hash the cave lattice corners spanning this slice once, rather than eight times per block.
        var lattice = new VoxelWorld.CaveLattice(world, baseX, baseY, baseZ,
                baseX + SIZE - 1, baseY + SIZE - 1, baseZ + SIZE - 1);

        var blocks = new byte[SIZE * AREA];

        for (int y = 0; y < SIZE; y++) {
            int blockY = baseY + y;

            for (int z = 0; z < SIZE; z++) {
                int blockZ = baseZ + z;
                int row = (z * SIZE);

                // One evaluator per row of x, so the y/z half of the noise is computed once instead of per block.
                var caveRow = world.caveRow(lattice, blockY, blockZ);

                for (int x = 0; x < SIZE; x++) {
                    blocks[(y * AREA) + row + x] = (byte) world.blockAt(baseX + x, blockY, blockZ,
                            heights[row + x], caveRow);
                }
            }
        }

        this.blocks = blocks;
        this.empty = false;
    }

    /** {@return the block at a section-local position, from -1 to 16 inclusive (the border is addressable)} */
    public int get(int x, int y, int z) {
        if (this.empty) {
            return VoxelWorld.AIR;
        }

        return this.blocks[((y + 1) * AREA) + ((z + 1) * SIZE) + (x + 1)];
    }

    /** {@return whether this section and its border are entirely air} */
    public boolean isEmpty() {
        return this.empty;
    }

    /** Runs production's {@link SectionVisibilityBuilder} over this slice's blocks. */
    public long computeVisibility() {
        if (this.empty) {
            return VisibilityEncoding.EVERYTHING;
        }

        var occluder = new SectionVisibilityBuilder();
        byte[] blocks = this.blocks;

        // Walk the backing array directly: get() would redo the border offset arithmetic 4096 times per section.
        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                int row = ((y + 1) * AREA) + ((z + 1) * SIZE) + 1;

                for (int x = 0; x < 16; x++) {
                    if (VoxelWorld.isOpaque(blocks[row + x])) {
                        occluder.markOpaque(x, y, z);
                    }
                }
            }
        }

        return occluder.computeVisibilityEncoding();
    }
}
