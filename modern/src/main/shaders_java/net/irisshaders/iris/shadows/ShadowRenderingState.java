package net.irisshaders.iris.shadows;

import com.mojang.blaze3d.platform.GlStateManager;

public class ShadowRenderingState {
	public static boolean areShadowsCurrentlyBeingRendered() {
		return ShadowRenderer.ACTIVE;
	}

	public static int getRenderDistance() {
		return ShadowRenderer.renderDistance;
	}

	/**
	 * Sets the viewport to cover the shadow map being rendered.
	 */
	public static void applyViewport() {
		GlStateManager._viewport(0, 0, ShadowRenderer.RESOLUTION, ShadowRenderer.RESOLUTION);
	}
}
