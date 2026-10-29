package net.irisshaders.iris.pathways.scaling;

import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.gui.option.IrisVideoSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import org.lwjgl.opengl.GL30C;

public final class RenderScale {
	private static MainTarget worldTarget;
	private static FsrUpscaler fsr;
	private static boolean fsrUnavailable;
	private static boolean active;

	private RenderScale() {
	}

	public static boolean isActive() {
		return active;
	}

	public static void setActive(boolean active) {
		RenderScale.active = active;
	}

	public static RenderTarget getWorldTarget() {
		return worldTarget;
	}

	private static float effectiveScale() {
		if (!IrisApi.getInstance().isShaderPackInUse()) {
			return 1.0f;
		}

		return IrisVideoSettings.renderScale.getScale();
	}

	/**
	 * Binds the scaled world target, creating or resizing it as needed. Called at the head of
	 * {@code GameRenderer.renderLevel}, which is before {@code LevelRenderer.renderLevel}
	 * clears the bound framebuffer and before Iris sizes its own render targets.
	 */
	public static void begin() {
		if (active) {
			// Defensive: a previous frame failed to resolve. Don't compound it.
			return;
		}

		float scale = effectiveScale();

		if (scale >= 1.0f) {
			releaseTargets();
			return;
		}

		// active is still false, so this is Minecraft's real render target.
		RenderTarget main = Minecraft.getInstance().getMainRenderTarget();

		int width = Math.max(1, Mth.ceil(main.width * scale));
		int height = Math.max(1, Mth.ceil(main.height * scale));

		if (worldTarget == null) {
			// MainTarget rather than TextureTarget so that the colour and depth attachments,
			// including the driver-specific fallbacks in allocateAttachments, match what
			// Minecraft would have allocated itself.
			worldTarget = new MainTarget(width, height);
			worldTarget.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		} else if (worldTarget.width != width || worldTarget.height != height) {
			worldTarget.resize(width, height, Minecraft.ON_OSX);
		}

		//? if forgelike {
		if (main.isStencilEnabled() && !worldTarget.isStencilEnabled()) {
			worldTarget.enableStencil();
		}
		//?}

		active = true;
		worldTarget.bindWrite(true);
	}

	/**
	 * Resolves the scaled world image into Minecraft's real main render target and leaves it
	 * bound. Called at the tail of {@code GameRenderer.renderLevel}: after the color space
	 * pass, which operates on the scaled image, and before the entity outline blit, the
	 * post-processing chain and the GUI, all of which run at native resolution.
	 */
	public static void end() {
		if (!active) {
			return;
		}

		// Flip this first so the getter below resolves to Minecraft's real target.
		active = false;

		RenderTarget main = Minecraft.getInstance().getMainRenderTarget();

		if (IrisVideoSettings.upscaleMode == UpscaleMode.FSR && !fsrUnavailable) {
			if (fsr == null) {
				fsr = FsrUpscaler.createOrNull(main.width, main.height);
				fsrUnavailable = fsr == null;
			}

			if (fsr != null) {
				fsr.process(worldTarget, main, sharpnessStops());
				main.bindWrite(true);
				return;
			}
		}

		IrisRenderSystem.blitFramebuffer(worldTarget.frameBufferId, main.frameBufferId,
			0, 0, worldTarget.width, worldTarget.height,
			0, 0, main.width, main.height,
			GL30C.GL_COLOR_BUFFER_BIT, GL30C.GL_LINEAR);

		main.bindWrite(true);
	}

	private static float sharpnessStops() {
		int sharpness = Mth.clamp(IrisVideoSettings.upscaleSharpness, 0, 100);
		return (100 - sharpness) * 0.02f;
	}

	private static void releaseTargets() {
		if (worldTarget != null) {
			worldTarget.destroyBuffers();
			worldTarget = null;
		}

		if (fsr != null) {
			fsr.destroy();
			fsr = null;
		}

		fsrUnavailable = false;
	}

	public static void onSettingsChanged() {
		if (fsr != null) {
			fsr.destroy();
			fsr = null;
		}

		fsrUnavailable = false;
	}
}
