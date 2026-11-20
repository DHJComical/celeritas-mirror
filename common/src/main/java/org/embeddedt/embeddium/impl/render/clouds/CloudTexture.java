package org.embeddedt.embeddium.impl.render.clouds;

/**
 * The cells of the clouds texture, one per pixel.
 */
public final class CloudTexture {
    static final int OPEN_NORTH = 1;
    static final int OPEN_SOUTH = 2;
    static final int OPEN_WEST = 4;
    static final int OPEN_EAST = 8;
    static final int FILLED = 16;

    final int width, height;
    final int[] pixels;
    final byte[] cells;

    /**
     * @param pixels the pixels in rows along X, packed as ABGR (as {@code NativeImage#getPixelRGBA} returns them)
     */
    public CloudTexture(int width, int height, int[] pixels) {
        if (pixels.length != width * height) {
            throw new IllegalArgumentException("Expected " + (width * height) + " pixels, got " + pixels.length);
        }

        this.width = width;
        this.height = height;
        this.pixels = pixels;
        this.cells = new byte[pixels.length];

        for (int z = 0; z < height; z++) {
            for (int x = 0; x < width; x++) {
                int pixel = this.pixel(x, z);

                if (!isFilled(pixel)) {
                    continue;
                }

                // A side is open where the neighbor differs. Between two filled cells of different colors, both
                // sides are open, and the renderer only draws the one facing the camera.
                int flags = FILLED;
                int north = this.pixel(x, z - 1);
                int south = this.pixel(x, z + 1);
                int west = this.pixel(x - 1, z);
                int east = this.pixel(x + 1, z);

                if (north != pixel) {
                    flags |= OPEN_NORTH;
                }

                if (south != pixel) {
                    flags |= OPEN_SOUTH;
                }

                if (west != pixel) {
                    flags |= OPEN_WEST;
                }

                if (east != pixel) {
                    flags |= OPEN_EAST;
                }

                this.cells[x + z * width] = (byte) flags;
            }
        }
    }

    int cell(int x, int z) {
        return this.cells[Math.floorMod(x, this.width) + Math.floorMod(z, this.height) * this.width];
    }

    private int pixel(int x, int z) {
        return this.pixels[Math.floorMod(x, this.width) + Math.floorMod(z, this.height) * this.width];
    }

    private static boolean isFilled(int abgr) {
        return (abgr >>> 24) > 1;
    }
}
