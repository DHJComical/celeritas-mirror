package org.embeddedt.embeddium.impl.model.light.data;

import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.embeddedt.embeddium.impl.util.position.SectionPos;

import java.util.Arrays;

/**
 * The light data cache is used to make accessing the light data and occlusion properties of blocks cheaper. The data
 * for each block is stored as an integer with packed fields in order to work around the lack of value types in Java.
 *
 * This code is not very pretty, but it does perform significantly faster than the vanilla implementation and has
 * good cache locality.
 *
 * Each integer contains the following fields:
 * - BL: World block light, encoded as a 4-bit unsigned integer
 * - SL: World sky light, encoded as a 4-bit unsigned integer
 * - LU: Block luminance, encoded as a 4-bit unsigned integer
 * - AO: Ambient occlusion, floating point value in the range of 0.0..1.0 encoded as a 16-bit unsigned integer with 12-bit precision
 * - EM: Emissive test, true if block uses emissive lighting
 * - OP: Block opacity test, true if opaque
 * - FO: Full cube opacity test, true if opaque full cube
 * - FC: Full cube test, true if full cube
 *
 * You can use the various static pack/unpack methods to extract these values in a usable format.
 */
public abstract class LightDataAccess {
    private static final int NEIGHBOR_BLOCK_RADIUS = 2;
    private static final int BLOCK_LENGTH = 16 + (NEIGHBOR_BLOCK_RADIUS * 2);

    private final int[] light;

    private int xOffset, yOffset, zOffset;

    public LightDataAccess() {
        this.light = new int[BLOCK_LENGTH * BLOCK_LENGTH * BLOCK_LENGTH];
    }

    public void reset(int minBlockX, int minBlockY, int minBlockZ) {
        this.xOffset = minBlockX - NEIGHBOR_BLOCK_RADIUS;
        this.yOffset = minBlockY - NEIGHBOR_BLOCK_RADIUS;
        this.zOffset = minBlockZ - NEIGHBOR_BLOCK_RADIUS;

        Arrays.fill(this.light, 0);
    }

    private int index(int x, int y, int z) {
        int x2 = x - this.xOffset;
        int y2 = y - this.yOffset;
        int z2 = z - this.zOffset;

        return (z2 * BLOCK_LENGTH * BLOCK_LENGTH) + (y2 * BLOCK_LENGTH) + x2;
    }

    protected abstract int compute(int x, int y, int z);

    /**
     * Returns the light data for the block at the given position. The property fields can then be accessed using
     * the various unpack methods below.
     */
    public int get(int x, int y, int z) {
        int l = this.index(x, y, z);

        int word = this.light[l];

        if (word != 0) {
            return word;
        }

        return this.light[l] = this.compute(x, y, z);
    }

    public int get(int x, int y, int z, ModelQuadFacing d1, ModelQuadFacing d2) {
        return this.get(x + d1.getStepX() + d2.getStepX(),
                y + d1.getStepY() + d2.getStepY(),
                z + d1.getStepZ() + d2.getStepZ());
    }

    public int get(int x, int y, int z, ModelQuadFacing dir) {
        return this.get(x + dir.getStepX(),
                y + dir.getStepY(),
                z + dir.getStepZ());
    }

    public static int packBL(int blockLight) {
        return blockLight & 0xF;
    }

    public static int unpackBL(int word) {
        return word & 0xF;
    }

    public static int packSL(int skyLight) {
        return (skyLight & 0xF) << 4;
    }

    public static int unpackSL(int word) {
        return (word >>> 4) & 0xF;
    }

    public static int packLU(int luminance) {
        return (luminance & 0xF) << 8;
    }

    public static int unpackLU(int word) {
        return (word >>> 8) & 0xF;
    }

    public static int packAO(float ao) {
        int aoi = (int) (ao * 4096.0f);
        return (aoi & 0xFFFF) << 12;
    }

    public static float unpackAO(int word) {
        int aoi = (word >>> 12) & 0xFFFF;
        return aoi * (1.0f / 4096.0f);
    }

    public static int packEM(boolean emissive) {
        return (emissive ? 1 : 0) << 28;
    }

    public static boolean unpackEM(int word) {
        return ((word >>> 28) & 0b1) != 0;
    }

    public static int packOP(boolean opaque) {
        return (opaque ? 1 : 0) << 29;
    }

    public static boolean unpackOP(int word) {
        return ((word >>> 29) & 0b1) != 0;
    }

    public static int packFO(boolean opaque) {
        return (opaque ? 1 : 0) << 30;
    }

    public static boolean unpackFO(int word) {
        return ((word >>> 30) & 0b1) != 0;
    }

    public static int packFC(boolean fullCube) {
        return (fullCube ? 1 : 0) << 31;
    }

    public static boolean unpackFC(int word) {
        return ((word >>> 31) & 0b1) != 0;
    }

    public static int pack(int block, int sky) {
        return block << 4 | sky << 20;
    }

    public static int unpackBlock(int packed) {
        return (packed & 0xFFFF) >> 4;
    }

    public static int unpackSky(int packed) {
        return (packed >> 20) & 0xFFFF;
    }

    /**
     * Computes the combined lightmap using block light, sky light, and luminance values.
     *
     * <p>This method's logic is equivalent to
     * {@link LevelRenderer#getLightColor(BlockAndTintGetter, BlockPos)}, but without the
     * emissive check.
     */
    public static int getLightmap(int word) {
        return pack(Math.max(unpackBL(word), unpackLU(word)), unpackSL(word));
    }

    public static final int FULL_BRIGHT = pack(15, 15);

    /**
     * Like {@link #getLightmap(int)}, but checks {@link #unpackEM(int)} first and returns
     * the {@link LightTexture#FULL_BRIGHT fullbright lightmap} if emissive.
     *
     * <p>This method's logic is equivalent to
     * {@link LevelRenderer#getLightColor(BlockAndTintGetter, BlockPos)}.
     */
    public static int getEmissiveLightmap(int word) {
        if (unpackEM(word)) {
            return FULL_BRIGHT;
        } else {
            return getLightmap(word);
        }
    }
}