package net.irisshaders.iris.shadows.frustum;

public abstract class CommonFrustum {
    public void prepare(double camX, double camY, double camZ) {
        // Do nothing by default
    }
    abstract public boolean isVisible(MCAABB aabb);
}
