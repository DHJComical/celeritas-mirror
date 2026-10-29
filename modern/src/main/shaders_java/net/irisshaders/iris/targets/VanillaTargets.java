package net.irisshaders.iris.targets;

import com.mojang.blaze3d.pipeline.RenderTarget;
//? if >=26.1 {
/*import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL30C;
*///?}

/**
 * The GL names behind vanilla's render targets, and binding them for drawing.
 */
public final class VanillaTargets {
	private VanillaTargets() {
	}

	public static int colorTexture(RenderTarget target) {
		//? if <26.1 {
		return target.getColorTextureId();
		//?} else
		//return ((GlTexture) target.getColorTexture()).glId();
	}

	public static int depthTexture(RenderTarget target) {
		//? if <26.1 {
		return target.getDepthTextureId();
		//?} else
		//return ((GlTexture) target.getDepthTexture()).glId();
	}

	public static int framebuffer(RenderTarget target) {
		//? if <26.1 {
		return target.frameBufferId;
		//?} else {
		/*GlDevice device = (GlDevice) RenderSystem.getDevice().backend;
		return ((GlTexture) target.getColorTexture()).getFbo(device.directStateAccess(), target.getDepthTexture());
		*///?}
	}

	/**
	 * Binds the target for drawing, and sets the viewport to cover it if requested.
	 */
	public static void bind(RenderTarget target, boolean setViewport) {
		//? if <26.1 {
		target.bindWrite(setViewport);
		//?} else {
		/*GlStateManager._glBindFramebuffer(GL30C.GL_FRAMEBUFFER, framebuffer(target));

		if (setViewport) {
			GlStateManager._viewport(0, 0, target.width, target.height);
		}
		*///?}
	}
}
