package org.embeddedt.embeddium.impl.render.chunk.occlusion;

import grondag.bitraster.PackedBox;

final class RenderableBounds {
    private int minX = 16, minY = 16, minZ = 16;
    private int maxX = -1, maxY = -1, maxZ = -1;

    void mark(int x, int y, int z) {
        if (x < this.minX) this.minX = x;
        if (y < this.minY) this.minY = y;
        if (z < this.minZ) this.minZ = z;
        if (x > this.maxX) this.maxX = x;
        if (y > this.maxY) this.maxY = y;
        if (z > this.maxZ) this.maxZ = z;
    }

    boolean isEmpty() {
        return this.maxX < 0;
    }

    int packed() {
        if (isEmpty()) {
            return PackedBox.FULL_BOX;
        }

        return PackedBox.pack(this.minX, this.minY, this.minZ,
                this.maxX + 1, this.maxY + 1, this.maxZ + 1, PackedBox.RANGE_EXTREME);
    }
}
