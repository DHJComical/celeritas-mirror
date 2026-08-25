package org.embeddedt.embeddium.impl.render.chunk.multidraw;

import org.embeddedt.embeddium.impl.gl.device.MultiDrawBatch;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class CachedBatch {
    private final @Nullable MultiDrawBatch batch;

    private final byte[] sections;

    private final int cameraMinX, cameraMaxX;
    private final int cameraMinY, cameraMaxY;
    private final int cameraMinZ, cameraMaxZ;

    public CachedBatch(@Nullable MultiDrawBatch batch,
                       byte[] sections, int sectionCount,
                       int cameraMinX, int cameraMaxX,
                       int cameraMinY, int cameraMaxY,
                       int cameraMinZ, int cameraMaxZ) {
        this.batch = batch;
        this.sections = Arrays.copyOf(sections, sectionCount);
        this.cameraMinX = cameraMinX;
        this.cameraMaxX = cameraMaxX;
        this.cameraMinY = cameraMinY;
        this.cameraMaxY = cameraMaxY;
        this.cameraMinZ = cameraMinZ;
        this.cameraMaxZ = cameraMaxZ;
    }

    public boolean isValidFor(byte[] sections, int sectionCount, int cameraX, int cameraY, int cameraZ) {
        return cameraX >= this.cameraMinX && cameraX < this.cameraMaxX
                && cameraY >= this.cameraMinY && cameraY < this.cameraMaxY
                && cameraZ >= this.cameraMinZ && cameraZ < this.cameraMaxZ
                && sectionCount == this.sections.length
                && Arrays.equals(this.sections, 0, sectionCount, sections, 0, sectionCount);
    }

    public @Nullable MultiDrawBatch getBatch() {
        return this.batch;
    }

    public void delete() {
        if (this.batch != null) {
            this.batch.delete();
        }
    }
}
