package net.irisshaders.iris.shadows;

public class ShadowRenderingState {
	public static boolean areShadowsCurrentlyBeingRendered() {
		return CommonShadowRenderer.ACTIVE;
	}

	public static int getRenderDistance() {
		return CommonShadowRenderer.renderDistance;
	}
}
