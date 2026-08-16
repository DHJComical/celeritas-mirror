package org.embeddedt.embeddium.impl.render.viewport.frustum;

public interface Frustum {
    /**
     * Returned when the box is known to be fully inside the frustum.
     */
    int FULLY_INSIDE = 0;
    /**
     * Returned when the box is at least partially within the frustum.
     */
    int PARTIALLY_INSIDE = 1;
    /**
     * Returned when the box is outside the frustum.
     */
    int OUTSIDE = 2;

    boolean testAab(float minX, float minY, float minZ, float maxX, float maxY, float maxZ);

    default int intersectAab(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        return testAab(minX, minY, minZ, maxX, maxY, maxZ) ? PARTIALLY_INSIDE : OUTSIDE;
    }
}
