package org.embeddedt.embeddium.impl.render.chunk.bench.voxel;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.bench.WorldType;

/**
 * A procedural block world: terrain with caves, evaluated on demand as a pure function of position. Nothing is
 * stored, so any subset can be meshed without materialising the rest, and content is identical across rebuilds.
 */
public final class VoxelWorld {
    public static final int AIR = 0;
    /** Opaque cube, drawn on the solid pass. */
    public static final int SOLID = 1;
    /** Non-opaque cube on the cutout pass (leaves etc.), so regions carry more than one unsorted pass. */
    public static final int CUTOUT = 2;
    /** Translucent (sorted) pass. */
    public static final int WATER = 3;

    /** Blocks below this height are flooded where the terrain leaves air. */
    public static final int SEA_LEVEL = 62;

    /** Cave noise lattice spacing in blocks. */
    private static final int CAVE_CELL = 28;

    /** Depth below the surface at which caves begin, keeping the terrain skin intact. */
    private static final int CAVE_MARGIN = 5;

    public static final long DEFAULT_SEED = 0x5EED_C0FFEE_1234L;

    /** Half-width of the noise band carved into caves. Calibrated against a real 1.20.1 overworld. */
    public static final double DEFAULT_CAVE_WIDTH = 0.03D;

    /** Fraction of terrain-skin blocks replaced by {@link #CUTOUT}, out of 256. Confined to the surface. */
    private static final int CUTOUT_ROLL = 40; // ~16% of the skin

    private final WorldType type;
    private final long seed;
    private final double caveWidth;

    /**
     * Column heightmaps by section column, so the 324 sines a slice needs are paid once per column instead of
     * once per section. Not thread safe: a thread that builds slices concurrently needs its own
     * {@code VoxelWorld}, which is free to do since the same seed always generates the same blocks.
     */
    private final Long2ObjectOpenHashMap<int[]> columnHeights = new Long2ObjectOpenHashMap<>();

    public VoxelWorld(WorldType type) {
        this(type, DEFAULT_SEED, DEFAULT_CAVE_WIDTH);
    }

    public VoxelWorld(WorldType type, long seed, double caveWidth) {
        this.type = type;
        this.seed = seed;
        this.caveWidth = caveWidth;
    }

    public int blockAt(int bx, int by, int bz) {
        return this.blockAt(bx, by, bz, this.surfaceHeight(bx, bz), this.caveRow(null, by, bz));
    }

    /**
     * Overload for {@link VoxelSlice}'s inner loop, with the column height pre-computed and the cave noise
     * bound to the row this block sits on.
     */
    int blockAt(int bx, int by, int bz, int surface, CaveRow row) {
        if (this.type == WorldType.EMPTY) {
            return AIR;
        }

        if (by >= surface) {
            return by < SEA_LEVEL ? WATER : AIR;
        }

        if (by < surface - CAVE_MARGIN) {
            return row.isCave(bx) ? AIR : SOLID;
        }

        return isCutout(bx, by, bz) ? CUTOUT : SOLID;
    }

    /**
     * {@return a cave-noise evaluator for the row of blocks at {@code (by, bz)} running along x}
     *
     * A null {@code lattice} hashes the noise corners on demand.
     */
    CaveRow caveRow(CaveLattice lattice, int by, int bz) {
        return new CaveRow(this, lattice, by, bz);
    }

    /** {@return whether this world has no blocks at all} */
    boolean isVoid() {
        return this.type == WorldType.EMPTY;
    }

    /** {@return a dense view of one section and its one-block border} */
    public VoxelSlice slice(int chunkX, int chunkY, int chunkZ) {
        return new VoxelSlice(this, chunkX, chunkY, chunkZ);
    }

    /** {@return whether this block stops light for the visibility flood fill} */
    public static boolean isOpaque(int block) {
        return block == SOLID;
    }

    /** {@return whether this block has geometry to draw} */
    public static boolean isRenderable(int block) {
        return block != AIR;
    }

    /** {@return whether a face of {@code block} pointing at {@code neighbour} is exposed} */
    public static boolean isFaceVisible(int block, int neighbour) {
        if (isOpaque(neighbour)) {
            return false;
        }

        return block != neighbour;
    }

    /**
     * {@return the heightmap for one section column, covering the section and its one-block border}
     *
     * Indexed as {@code (z * VoxelSlice.SIZE) + x}; the trailing element is the column maximum. Cached, since
     * every section in a column shares one heightmap.
     */
    int[] columnHeights(int chunkX, int chunkZ) {
        long key = (((long) chunkX) << 32) | (chunkZ & 0xFFFFFFFFL);
        int[] cached = this.columnHeights.get(key);

        if (cached != null) {
            return cached;
        }

        final int baseX = (chunkX << 4) - 1;
        final int baseZ = (chunkZ << 4) - 1;

        var heights = new int[VoxelSlice.AREA + 1];
        int maxHeight = Integer.MIN_VALUE;

        for (int z = 0; z < VoxelSlice.SIZE; z++) {
            for (int x = 0; x < VoxelSlice.SIZE; x++) {
                int height = this.surfaceHeight(baseX + x, baseZ + z);
                heights[(z * VoxelSlice.SIZE) + x] = height;
                maxHeight = Math.max(maxHeight, height);
            }
        }

        heights[VoxelSlice.AREA] = maxHeight;
        this.columnHeights.put(key, heights);

        return heights;
    }

    /** Height of the first air/water block in this column. Same heightmap as {@code SyntheticWorld.surfaceSectionY}. */
    public int surfaceHeight(int bx, int bz) {
        if (this.type == WorldType.CAVES) {
            return Integer.MAX_VALUE;
        }

        return (int) Math.floor(sectionHeight(bx * (1.0 / 16.0), bz * (1.0 / 16.0)) * 16.0);
    }

    /** Heightmap in section units, shared with {@code SyntheticWorld.surfaceSectionY}. */
    public static double sectionHeight(double x, double z) {
        return 4.0
                + 1.4 * Math.sin(x * 0.11)
                + 1.1 * Math.sin(z * 0.13)
                + 0.7 * Math.sin((x + z) * 0.07);
    }

    // ------------------------------------------------------------------
    // Noise
    // ------------------------------------------------------------------

    /**
     * Carves where trilinearly interpolated value noise crosses the middle of its range, along one row of
     * blocks in x. Everything invariant along the row — the y and z cells and their smoothstep weights — is
     * computed once here, and the eight cell corners are reloaded only when x crosses a cell boundary, which
     * happens every {@link #CAVE_CELL} blocks.
     *
     * Trilinear interpolation is symmetric in the order the three axes are blended, so the y and z blends are
     * folded into the two x-facing corner values when a cell is loaded, leaving one interpolation per block.
     * Floating-point addition is not associative, so in principle this can flip a block sitting within an ulp
     * of the cave threshold; in practice no divergence from the x-first order has been observed, over 3.7M
     * sampled blocks and the visibility encoding of a full render-distance-32 world.
     */
    static final class CaveRow {
        private final VoxelWorld world;
        private final CaveLattice lattice;
        private final double caveWidth;

        private final int cellY, cellZ;
        private final double ty, tz;

        /** The cell this row's corners are currently loaded for; no x is ever {@link Integer#MIN_VALUE}. */
        private int cellX = Integer.MIN_VALUE;

        /** The cell's two x faces, already blended over y and z. */
        private double near, far;

        CaveRow(VoxelWorld world, CaveLattice lattice, int by, int bz) {
            this.world = world;
            this.lattice = lattice;
            this.caveWidth = world.caveWidth;

            this.cellY = Math.floorDiv(by, CAVE_CELL);
            this.cellZ = Math.floorDiv(bz, CAVE_CELL);

            this.ty = smoothstep((by - (this.cellY * CAVE_CELL)) * (1.0 / CAVE_CELL));
            this.tz = smoothstep((bz - (this.cellZ * CAVE_CELL)) * (1.0 / CAVE_CELL));
        }

        boolean isCave(int bx) {
            int cx = Math.floorDiv(bx, CAVE_CELL);

            if (cx != this.cellX) {
                this.loadCell(cx);
            }

            // floorMod(bx, CAVE_CELL), without a second branchy division.
            double tx = smoothstep((bx - (cx * CAVE_CELL)) * (1.0 / CAVE_CELL));

            return Math.abs(lerp(this.near, this.far, tx) - 0.5) < this.caveWidth;
        }

        private void loadCell(int cx) {
            int cy = this.cellY, cz = this.cellZ;

            double c000 = this.corner(cx, cy, cz);
            double c100 = this.corner(cx + 1, cy, cz);
            double c010 = this.corner(cx, cy + 1, cz);
            double c110 = this.corner(cx + 1, cy + 1, cz);
            double c001 = this.corner(cx, cy, cz + 1);
            double c101 = this.corner(cx + 1, cy, cz + 1);
            double c011 = this.corner(cx, cy + 1, cz + 1);
            double c111 = this.corner(cx + 1, cy + 1, cz + 1);

            this.near = lerp(lerp(c000, c010, this.ty), lerp(c001, c011, this.ty), this.tz);
            this.far = lerp(lerp(c100, c110, this.ty), lerp(c101, c111, this.ty), this.tz);

            this.cellX = cx;
        }

        private double corner(int cx, int cy, int cz) {
            return this.lattice != null
                    ? this.lattice.get(cx, cy, cz)
                    : this.world.latticeValue(cx, cy, cz);
        }
    }

    /** A value in [0, 1), hashed from a lattice cell. */
    private double latticeValue(int cx, int cy, int cz) {
        return (mix(this.seed, cx, cy, cz) >>> 11) * 0x1.0p-53;
    }

    /**
     * The cave-noise lattice corners spanning one slice's block box. A slice is 18 blocks across and a lattice
     * cell is {@link #CAVE_CELL}, so this is at most 3x3x3 hashes standing in for eight per block.
     */
    static final class CaveLattice {
        private final int minCellX, minCellY, minCellZ;
        private final int strideY, strideZ;
        private final double[] values;

        /** Corners for the inclusive block box, plus the far corner each trilinear sample reaches for. */
        CaveLattice(VoxelWorld world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
            this.minCellX = Math.floorDiv(minX, CAVE_CELL);
            this.minCellY = Math.floorDiv(minY, CAVE_CELL);
            this.minCellZ = Math.floorDiv(minZ, CAVE_CELL);

            int countX = Math.floorDiv(maxX, CAVE_CELL) - this.minCellX + 2;
            int countY = Math.floorDiv(maxY, CAVE_CELL) - this.minCellY + 2;
            int countZ = Math.floorDiv(maxZ, CAVE_CELL) - this.minCellZ + 2;

            this.strideY = countX;
            this.strideZ = countX * countY;
            this.values = new double[countX * countY * countZ];

            for (int z = 0; z < countZ; z++) {
                for (int y = 0; y < countY; y++) {
                    for (int x = 0; x < countX; x++) {
                        this.values[(z * this.strideZ) + (y * this.strideY) + x] =
                                world.latticeValue(this.minCellX + x, this.minCellY + y, this.minCellZ + z);
                    }
                }
            }
        }

        double get(int cx, int cy, int cz) {
            return this.values[((cz - this.minCellZ) * this.strideZ)
                    + ((cy - this.minCellY) * this.strideY)
                    + (cx - this.minCellX)];
        }
    }

    private static boolean isCutout(int bx, int by, int bz) {
        return ((int) (mix(0xC0FFEE_5EEDL, bx, by, bz) >>> 40) & 0xFF) < CUTOUT_ROLL;
    }

    /** splitmix64 finalizer over the position, matching the hashing style the rest of the harness uses. */
    private static long mix(long seed, int x, int y, int z) {
        long h = seed;
        h = h * 0x9E3779B97F4A7C15L + x * 0xBF58476D1CE4E5B9L;
        h = h * 0x9E3779B97F4A7C15L + y * 0x94D049BB133111EBL;
        h = h * 0x9E3779B97F4A7C15L + z * 0xD6E8FEB86659FD93L;

        h ^= h >>> 30;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 27;
        h *= 0x94D049BB133111EBL;
        h ^= h >>> 31;

        return h;
    }

    private static double smoothstep(double t) {
        return t * t * (3.0 - (2.0 * t));
    }

    private static double lerp(double a, double b, double t) {
        return a + ((b - a) * t);
    }

    public WorldType getType() {
        return this.type;
    }

    public double getCaveWidth() {
        return this.caveWidth;
    }

    @Override
    public String toString() {
        return "VoxelWorld{type=" + this.type + ", caveWidth=" + this.caveWidth + "}";
    }
}
