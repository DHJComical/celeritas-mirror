package org.embeddedt.embeddium.impl.render.viewport;

import org.embeddedt.embeddium.impl.loader.common.LoaderServices;
import org.embeddedt.embeddium.impl.render.viewport.frustum.Frustum;
import net.minecraft.world.phys.AABB;
import org.embeddedt.embeddium.impl.util.WorldUtil;
import org.joml.RoundingMode;
import org.joml.Vector3d;
import org.joml.Vector3i;
import org.joml.Vector3ic;

public final class Viewport {
    private final Frustum frustum;
    private final CameraTransform transform;

    private final Vector3i chunkCoords;
    private final Vector3i blockCoords;

    public Viewport(Frustum frustum, Vector3d position) {
        this.frustum = frustum;
        this.transform = new CameraTransform(position.x, position.y, position.z);

        this.chunkCoords = new Vector3i(
                WorldUtil.posToSectionCoord(position.x),
                WorldUtil.posToSectionCoord(position.y),
                WorldUtil.posToSectionCoord(position.z)
        );

        this.blockCoords = new Vector3i(position.x, position.y, position.z, RoundingMode.FLOOR);
    }

    public boolean isBoxVisible(AABB box) {
        if (!LoaderServices.INSTANCE.isCullableAABB(box)) {
            return true;
        }

        return this.frustum.testAab(
                (float)(box.minX - this.transform.intX) - this.transform.fracX,
                (float)(box.minY - this.transform.intY) - this.transform.fracY,
                (float)(box.minZ - this.transform.intZ) - this.transform.fracZ,
                (float)(box.maxX - this.transform.intX) - this.transform.fracX,
                (float)(box.maxY - this.transform.intY) - this.transform.fracY,
                (float)(box.maxZ - this.transform.intZ) - this.transform.fracZ
        );
    }

    public boolean isBoxVisible(int intOriginX, int intOriginY, int intOriginZ, float floatSize) {
        return isBoxVisible(intOriginX, intOriginY, intOriginZ, floatSize, floatSize, floatSize);
    }

    public boolean isBoxVisible(int intOriginX, int intOriginY, int intOriginZ, float floatSizeX, float floatSizeY, float floatSizeZ) {
        float floatOriginX = (intOriginX - this.transform.intX) - this.transform.fracX;
        float floatOriginY = (intOriginY - this.transform.intY) - this.transform.fracY;
        float floatOriginZ = (intOriginZ - this.transform.intZ) - this.transform.fracZ;

        return this.frustum.testAab(
                floatOriginX - floatSizeX,
                floatOriginY - floatSizeY,
                floatOriginZ - floatSizeZ,

                floatOriginX + floatSizeX,
                floatOriginY + floatSizeY,
                floatOriginZ + floatSizeZ
        );
    }

    public CameraTransform getTransform() {
        return this.transform;
    }

    public Vector3ic getChunkCoord() {
        return this.chunkCoords;
    }

    public Vector3ic getBlockCoord() {
        return this.blockCoords;
    }
}
