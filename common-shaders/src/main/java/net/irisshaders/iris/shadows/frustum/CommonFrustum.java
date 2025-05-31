package net.irisshaders.iris.shadows.frustum;

import org.embeddedt.embeddium.impl.render.viewport.ViewportProvider;

public abstract class CommonFrustum implements ViewportProvider {
    public void prepare(double camX, double camY, double camZ) {
        // Do nothing by default
    }
    abstract public boolean isVisible(MCAABB aabb);
}
