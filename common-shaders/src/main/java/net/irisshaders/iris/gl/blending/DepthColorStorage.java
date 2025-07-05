package net.irisshaders.iris.gl.blending;

import static com.mitchej123.glsm.GLStateManagerService.GL_STATE_MANAGER;

public class DepthColorStorage {
	private static boolean originalDepthEnable;
	private static ColorMask originalColor;
	private static boolean depthColorLocked;

	public static boolean isDepthColorLocked() {
		return depthColorLocked;
	}

	public static void disableDepthColor() {
		if (!depthColorLocked) {
			// Only save the previous state if the depth and color mask wasn't already locked

			originalDepthEnable = GL_STATE_MANAGER.getDepthStateMask();
			originalColor = GL_STATE_MANAGER.getColorMask();
		}

		depthColorLocked = false;

		GL_STATE_MANAGER.glDepthMask(false);
		GL_STATE_MANAGER.glColorMask(false, false, false, false);

		depthColorLocked = true;
	}

	public static void deferDepthEnable(boolean enabled) {
		originalDepthEnable = enabled;
	}

	public static void deferColorMask(boolean red, boolean green, boolean blue, boolean alpha) {
		originalColor = new ColorMask(red, green, blue, alpha);
	}

	public static void unlockDepthColor() {
		if (!depthColorLocked) {
			return;
		}

		depthColorLocked = false;

		GL_STATE_MANAGER.glDepthMask(originalDepthEnable);

		GL_STATE_MANAGER.glColorMask(originalColor.isRedMasked(), originalColor.isGreenMasked(), originalColor.isBlueMasked(), originalColor.isAlphaMasked());
	}
}
