package net.irisshaders.iris.shadows.frustum;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import org.embeddedt.embeddium.impl.render.viewport.Viewport;
import org.embeddedt.embeddium.impl.render.viewport.ViewportProvider;
import org.joml.Matrix4f;
import org.joml.Vector3d;

public class CullEverythingFrustum extends Frustum implements ViewportProvider, org.embeddedt.embeddium.impl.render.viewport.frustum.Frustum {
	public CullEverythingFrustum() {
		super(new Matrix4f(), new Matrix4f());
	}

	// For Immersive Portals
	// We return false here since isVisible is going to return false anyways.
	public boolean canDetermineInvisible(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
		return false;
	}

	public boolean isVisible(AABB box) {
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
