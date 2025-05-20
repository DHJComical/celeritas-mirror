package net.irisshaders.iris.shadows.frustum;

public interface MCFrustum {
    void prepare(double camX, double camY, double camZ);
    boolean isVisible(MCAABB aabb);
}
