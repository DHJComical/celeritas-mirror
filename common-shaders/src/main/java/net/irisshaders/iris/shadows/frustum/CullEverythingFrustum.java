package net.irisshaders.iris.shadows.frustum;

import org.embeddedt.embeddium.impl.render.viewport.Viewport;
import org.embeddedt.embeddium.impl.render.viewport.ViewportProvider;
import org.joml.Vector3d;

public class CullEverythingFrustum extends CommonFrustum implements ViewportProvider, org.embeddedt.embeddium.impl.render.viewport.frustum.Frustum {
	public CullEverythingFrustum() {
    }

	// For Immersive Portals
	// We return false here since isVisible is going to return false anyways.
	public boolean canDetermineInvisible(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
		return false;
	}

	public boolean isVisible(MCAABB box) {
		return false;
	}

    private static final Vector3d EMPTY = new Vector3d();

    @Override
    public Viewport sodium$createViewport() {
        return new Viewport(this, EMPTY);
    }

    @Override
    public boolean testAab(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        return false;
    }
}
