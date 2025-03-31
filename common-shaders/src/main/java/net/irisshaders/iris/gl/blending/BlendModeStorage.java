package net.irisshaders.iris.gl.blending;

import static com.mitchej123.glsm.GLStateManagerService.GL_STATE_MANAGER;

import net.irisshaders.iris.gl.IrisRenderSystem;

public class BlendModeStorage {
	private static boolean originalBlendEnable;
	private static BlendMode originalBlend;
	private static boolean blendLocked;

	public static boolean isBlendLocked() {
		return blendLocked;
	}

	public static void overrideBlend(BlendMode override) {
		if (!blendLocked) {
			// Only save the previous state if the blend mode wasn't already locked

			originalBlendEnable = GL_STATE_MANAGER.isBlendEnabled();
			originalBlend = GL_STATE_MANAGER.getBlendMode();
		}

		blendLocked = false;

		if (override == null) {
			GL_STATE_MANAGER.disableBlend();
		} else {
			GL_STATE_MANAGER.enableBlend();
			GL_STATE_MANAGER.glBlendFuncSeparate(override.srcRgb(), override.dstRgb(), override.srcAlpha(), override.dstAlpha());
		}

		blendLocked = true;
	}

	public static void overrideBufferBlend(int index, BlendMode override) {
		if (!blendLocked) {
			// Only save the previous state if the blend mode wasn't already locked
            originalBlendEnable = GL_STATE_MANAGER.isBlendEnabled();
            originalBlend = GL_STATE_MANAGER.getBlendMode();
		}

		if (override == null) {
			IrisRenderSystem.disableBufferBlend(index);
		} else {
			IrisRenderSystem.enableBufferBlend(index);
			IrisRenderSystem.blendFuncSeparatei(index, override.srcRgb(), override.dstRgb(), override.srcAlpha(), override.dstAlpha());
		}

		blendLocked = true;
	}

	public static void deferBlendModeToggle(boolean enabled) {
		originalBlendEnable = enabled;
	}

	public static void deferBlendFunc(int srcRgb, int dstRgb, int srcAlpha, int dstAlpha) {
		originalBlend = new BlendMode(srcRgb, dstRgb, srcAlpha, dstAlpha);
	}

	public static void restoreBlend() {
		if (!blendLocked) {
			return;
		}

		blendLocked = false;

		if (originalBlendEnable) {
			GL_STATE_MANAGER.enableBlend();
		} else {
			GL_STATE_MANAGER.disableBlend();
		}

		GL_STATE_MANAGER.glBlendFuncSeparate(originalBlend.srcRgb(), originalBlend.dstRgb(),
			originalBlend.srcAlpha(), originalBlend.dstAlpha());
	}
}
