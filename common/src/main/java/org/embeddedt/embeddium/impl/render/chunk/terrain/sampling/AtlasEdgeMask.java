package org.embeddedt.embeddium.impl.render.chunk.terrain.sampling;

import org.embeddedt.embeddium.impl.common.util.NativeBuffer;
import org.embeddedt.embeddium.impl.gl.GlObject;
import org.jetbrains.annotations.Nullable;
import org.taumc.celeritas.lwjgl.GL11;
import org.taumc.celeritas.lwjgl.GL12;
import org.taumc.celeritas.lwjgl.GL30;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.taumc.celeritas.lwjgl.LWJGLServiceProvider.LWJGL;

/**
 * Lets the chunk shader use filtered sampling (RGSS, emulated nearest) on a block atlas without padding between
 * sprites. Bilinear fetches and RGSS taps read texels around the sample point, so near a sprite's edge they would
 * blend in the neighbouring sprite. The shader prevents this by clamping each fetch so its footprint stays inside the
 * sprite, which needs to know where the sprite ends.
 *
 * <p>Rather than storing per-sprite bounds, the atlas is split into cells of 2^mipLevels texels. The stitcher places
 * sprites on that grid, so a cell normally belongs to a single sprite. A fetch never reaches further than the cells next to
 * the fragment's own, so it is enough for each cell to record which of its four sides border a different sprite: the
 * shader looks up the fragment's cell and clamps only on those sides, inset by half a texel of the mip level being
 * read. Inside a sprite, fetches cross cell lines freely.</p>
 *
 * <p>Cells that are shared by sprites, only partly covered by one, or empty are marked {@link #FALLBACK}, and the
 * shader samples them as plain nearest filtering would. Neighbouring cells treat them as a border.</p>
 *
 * <p>{@link #upload()} must be called while the texture is bound to GL_TEXTURE_2D on the active unit.</p>
 */
public final class AtlasEdgeMask extends GlObject {
    // Must match the bit layout in terrain_sampling.glsl.
    public static final int BORDER_LEFT = 1;
    public static final int BORDER_RIGHT = 2;
    public static final int BORDER_TOP = 4;
    public static final int BORDER_BOTTOM = 8;
    public static final int FALLBACK = 16;

    private static final int NO_OWNER = -1;

    private final int atlasWidth, atlasHeight, mipLevels;
    private final int cellsX, cellsY;

    private byte[] pendingData;

    private AtlasEdgeMask(int atlasWidth, int atlasHeight, int mipLevels, int cellsX, int cellsY, byte[] data) {
        this.atlasWidth = atlasWidth;
        this.atlasHeight = atlasHeight;
        this.mipLevels = mipLevels;
        this.cellsX = cellsX;
        this.cellsY = cellsY;
        this.pendingData = data;
        this.setHandle(LWJGL.glGenTextures());
    }

    /**
     * @param spriteRects the x, y, width and height of every sprite in atlas texels, four ints per sprite
     * @return null if the atlas has no mipmaps
     */
    public static @Nullable AtlasEdgeMask create(int atlasWidth, int atlasHeight, int mipLevels, int[] spriteRects) {
        if (mipLevels < 1 || atlasWidth <= 0 || atlasHeight <= 0) {
            return null;
        }

        int cellMask = (1 << mipLevels) - 1;
        int cellsX = (atlasWidth + cellMask) >> mipLevels;
        int cellsY = (atlasHeight + cellMask) >> mipLevels;

        return new AtlasEdgeMask(atlasWidth, atlasHeight, mipLevels, cellsX, cellsY,
                computeCellFlags(cellsX, cellsY, mipLevels, spriteRects));
    }

    /**
     * {@return the flags of every cell, in rows from the top of the atlas}
     */
    static byte[] computeCellFlags(int cellsX, int cellsY, int mipLevels, int[] spriteRects) {
        int cellMask = (1 << mipLevels) - 1;

        int[] owners = new int[cellsX * cellsY];
        boolean[] fallback = new boolean[cellsX * cellsY];
        Arrays.fill(owners, NO_OWNER);

        for (int sprite = 0; sprite < spriteRects.length / 4; sprite++) {
            int x = spriteRects[sprite * 4], y = spriteRects[sprite * 4 + 1];
            int width = spriteRects[sprite * 4 + 2], height = spriteRects[sprite * 4 + 3];

            if (width <= 0 || height <= 0) {
                continue;
            }

            boolean aligned = (x & cellMask) == 0 && (y & cellMask) == 0;
            boolean partialX = ((x + width) & cellMask) != 0;
            boolean partialY = ((y + height) & cellMask) != 0;

            int cx0 = Math.max(0, x >> mipLevels), cy0 = Math.max(0, y >> mipLevels);
            int cx1 = Math.min(cellsX, (x + width + cellMask) >> mipLevels);
            int cy1 = Math.min(cellsY, (y + height + cellMask) >> mipLevels);

            for (int cy = cy0; cy < cy1; cy++) {
                for (int cx = cx0; cx < cx1; cx++) {
                    int cell = cy * cellsX + cx;

                    if (owners[cell] == NO_OWNER) {
                        owners[cell] = sprite;
                    } else {
                        fallback[cell] = true;
                    }

                    if (!aligned || (partialX && cx == cx1 - 1) || (partialY && cy == cy1 - 1)) {
                        fallback[cell] = true;
                    }
                }
            }
        }

        byte[] data = new byte[cellsX * cellsY];

        for (int cy = 0; cy < cellsY; cy++) {
            for (int cx = 0; cx < cellsX; cx++) {
                int cell = cy * cellsX + cx;

                if (owners[cell] == NO_OWNER || fallback[cell]) {
                    data[cell] = FALLBACK;
                    continue;
                }

                int flags = 0;

                if (!continuesSprite(owners, fallback, cellsX, cellsY, cell, cx - 1, cy)) {
                    flags |= BORDER_LEFT;
                }
                if (!continuesSprite(owners, fallback, cellsX, cellsY, cell, cx + 1, cy)) {
                    flags |= BORDER_RIGHT;
                }
                if (!continuesSprite(owners, fallback, cellsX, cellsY, cell, cx, cy - 1)) {
                    flags |= BORDER_TOP;
                }
                if (!continuesSprite(owners, fallback, cellsX, cellsY, cell, cx, cy + 1)) {
                    flags |= BORDER_BOTTOM;
                }

                data[cell] = (byte) flags;
            }
        }

        return data;
    }

    private static boolean continuesSprite(int[] owners, boolean[] fallback, int cellsX, int cellsY, int cell, int nx, int ny) {
        if (nx < 0 || ny < 0 || nx >= cellsX || ny >= cellsY) {
            return false;
        }

        int neighbor = ny * cellsX + nx;

        return owners[neighbor] == owners[cell] && !fallback[neighbor];
    }

    /**
     * Uploads the mask the first time it is called, and does nothing afterwards.
     */
    public void upload() {
        byte[] data = this.pendingData;

        if (data == null) {
            return;
        }

        this.pendingData = null;

        NativeBuffer buffer = new NativeBuffer(data.length);

        try {
            ByteBuffer pixels = buffer.getDirectBuffer();
            pixels.put(data);
            pixels.position(0);

            // The game leaves its own unpack state behind after uploading sprites.
            LWJGL.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
            LWJGL.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
            LWJGL.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
            LWJGL.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);

            LWJGL.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            LWJGL.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            LWJGL.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 0);
            LWJGL.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_R8UI, this.cellsX, this.cellsY, 0,
                    GL30.GL_RED_INTEGER, GL11.GL_UNSIGNED_BYTE, pixels);

            LWJGL.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 4);
        } finally {
            buffer.free();
        }
    }

    public int atlasWidth() {
        return this.atlasWidth;
    }

    public int atlasHeight() {
        return this.atlasHeight;
    }

    public int mipLevels() {
        return this.mipLevels;
    }

    public int cellSize() {
        return 1 << this.mipLevels;
    }

    public int cellsX() {
        return this.cellsX;
    }

    public int cellsY() {
        return this.cellsY;
    }

    @Override
    protected void destroyInternal() {
        LWJGL.glDeleteTextures(this.handle());
    }
}
