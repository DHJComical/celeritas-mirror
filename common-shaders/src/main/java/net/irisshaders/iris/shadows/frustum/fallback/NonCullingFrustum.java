package net.irisshaders.iris.shadows.frustum.fallback;

import net.irisshaders.iris.shadows.frustum.CommonFrustum;
import net.irisshaders.iris.shadows.frustum.MCAABB;
import org.embeddedt.embeddium.impl.render.viewport.Viewport;
import org.embeddedt.embeddium.impl.render.viewport.ViewportProvider;
import org.joml.Vector3d;

import static org.embeddedt.embeddium.compat.mc.MinecraftVersionShimService.MINECRAFT_SHIM;

public class NonCullingFrustum extends CommonFrustum implements ViewportProvider, org.embeddedt.embeddium.impl.render.viewport.frustum.Frustum {
	public NonCullingFrustum() {
	}

	// For Immersive Portals
	// NB: The shadow culling in Immersive Portals must be disabled, because when Advanced Shadow Frustum Culling
	//     is not active, we are at a point where we can make no assumptions how the shader pack uses the shadow
	//     pass beyond what it already tells us. So we cannot use any extra fancy culling methods.
	public boolean canDetermineInvisible(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
		return false;
	}

    @Override
	public boolean isVisible(MCAABB box) {
		return true;
	}

    private final Vector3d pos = new Vector3d();

    @Override
    public Viewport sodium$createViewport() {
        return new Viewport(this, pos.set(MINECRAFT_SHIM.getUnshiftedCameraPosition()));
    }

    @Override
    public boolean testAab(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        return true;
    }
}
