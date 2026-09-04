package net.irisshaders.iris.targets;

import com.mojang.blaze3d.platform.GlStateManager;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.gl.texture.InternalTextureFormat;
import net.irisshaders.iris.gl.texture.PixelFormat;
import net.irisshaders.iris.gl.texture.PixelType;
import org.embeddedt.embeddium.impl.gl.debug.GLDebug;
import org.joml.Vector2i;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL13C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL43C;

import java.nio.ByteBuffer;
import java.util.function.BooleanSupplier;

public class RenderTarget {
	private static final ByteBuffer NULL_BUFFER = null;
	private final InternalTextureFormat internalFormat;
	private final PixelFormat format;
	private final PixelType type;
	private final int mainTexture;
	private final String name;
	private int altTexture;
	private int width;
	private int height;
	private boolean isValid;
	private BooleanSupplier oddParity;

	public RenderTarget(Builder builder) {
		this.isValid = true;

		this.internalFormat = builder.internalFormat;
		this.format = builder.format;
		this.type = builder.type;
		this.name = builder.name;

		this.width = builder.width;
		this.height = builder.height;

		this.mainTexture = GlStateManager._genTexture();
		this.altTexture = 0;

		setupTexture(mainTexture, builder.width, builder.height, allowsLinear());

		if (builder.name != null) {
			GLDebug.nameObject(GL43C.GL_TEXTURE, mainTexture, builder.name + " main");
		}

		// Clean up after ourselves
		// This is strictly defensive to ensure that other buggy code doesn't tamper with our textures
		GlStateManager._bindTexture(0);
	}

	private boolean allowsLinear() {
		return !internalFormat.getPixelFormat().isInteger();
	}

	private void allocateAltTexture() {
		this.altTexture = GlStateManager._genTexture();

		setupTexture(altTexture, width, height, allowsLinear());

		if (name != null) {
			GLDebug.nameObject(GL43C.GL_TEXTURE, altTexture, name + " alt");
		}

		GlStateManager._bindTexture(0);
	}

	public static Builder builder() {
		return new Builder();
	}

	private void setupTexture(int texture, int width, int height, boolean allowsLinear) {
		resizeTexture(texture, width, height);

		IrisRenderSystem.texParameteri(texture, GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MIN_FILTER, allowsLinear ? GL11C.GL_LINEAR : GL11C.GL_NEAREST);
		IrisRenderSystem.texParameteri(texture, GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MAG_FILTER, allowsLinear ? GL11C.GL_LINEAR : GL11C.GL_NEAREST);
		IrisRenderSystem.texParameteri(texture, GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_S, GL13C.GL_CLAMP_TO_EDGE);
		IrisRenderSystem.texParameteri(texture, GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_T, GL13C.GL_CLAMP_TO_EDGE);
	}

	private void resizeTexture(int texture, int width, int height) {
		IrisRenderSystem.texImage2D(texture, GL11C.GL_TEXTURE_2D, 0, internalFormat.getGlFormat(), width, height, 0, format.getGlFormat(), type.getGlFormat(), NULL_BUFFER);
	}

	void resize(Vector2i textureScaleOverride) {
		this.resize(textureScaleOverride.x, textureScaleOverride.y);
	}

	// Package private, call CompositeRenderTargets#resizeIfNeeded instead.
	void resize(int width, int height) {
		requireValid();

		this.width = width;
		this.height = height;

		resizeTexture(mainTexture, width, height);

		if (altTexture != 0) {
			resizeTexture(altTexture, width, height);
		}
	}

	public InternalTextureFormat getInternalFormat() {
		return internalFormat;
	}

	public int getMainTexture() {
		return getTextureForParity(true, isOddParity());
	}

	public int getAltTexture() {
		return getTextureForParity(false, isOddParity());
	}

	private boolean isOddParity() {
		return oddParity != null && oddParity.getAsBoolean();
	}

	public boolean isParityEnabled() {
		return oddParity != null;
	}

	/**
	 * Resolves one of the two textures by role, for a given frame parity. If parity is not enabled for this target,
	 * the roles are fixed and the parity is ignored.
	 *
	 * @param main whether the main (rather than the alternate) texture is wanted
	 * @param odd  whether to resolve for the odd frame parity
	 */
	public int getTextureForParity(boolean main, boolean odd) {
		requireValid();

		boolean rolesSwapped = isParityEnabled() && odd;

		if (main != rolesSwapped) {
			return mainTexture;
		}

		if (altTexture == 0) {
			allocateAltTexture();
		}

		return altTexture;
	}

	/**
	 * Enables role swapping across frames for this target, so that the texture holding the newest content is always
	 * the one the next frame will treat as the main texture. Only meaningful for targets that end the frame flipped;
	 * the alternative is copying the content back every frame.
	 */
	void enableParity(BooleanSupplier oddParity) {
		this.oddParity = oddParity;

		if (altTexture == 0) {
			// Both textures are in active use from here on out.
			allocateAltTexture();
		}
	}

	/**
	 * Resets the sampling mode of both textures, undoing any mipmapped filtering set up for this frame.
	 */
	public void resetSamplingMode() {
		int filter = allowsLinear() ? GL20C.GL_LINEAR : GL20C.GL_NEAREST;

		IrisRenderSystem.texParameteri(mainTexture, GL20C.GL_TEXTURE_2D, GL20C.GL_TEXTURE_MIN_FILTER, filter);

		if (altTexture != 0) {
			IrisRenderSystem.texParameteri(altTexture, GL20C.GL_TEXTURE_2D, GL20C.GL_TEXTURE_MIN_FILTER, filter);
		}
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public void destroy() {
		requireValid();
		isValid = false;

		GlStateManager._deleteTexture(mainTexture);

		if (altTexture != 0) {
			GlStateManager._deleteTexture(altTexture);
		}
	}

	private void requireValid() {
		if (!isValid) {
			throw new IllegalStateException("Attempted to use a deleted composite render target");
		}
	}

	public static class Builder {
		private InternalTextureFormat internalFormat = InternalTextureFormat.RGBA8;
		private int width = 0;
		private int height = 0;
		private PixelFormat format = PixelFormat.RGBA;
		private PixelType type = PixelType.UNSIGNED_BYTE;
		private String name = null;

		private Builder() {
			// No-op
		}

		public Builder setName(String name) {
			this.name = name;

			return this;
		}

		public Builder setInternalFormat(InternalTextureFormat format) {
			this.internalFormat = format;

			return this;
		}

		public Builder setDimensions(int width, int height) {
			if (width <= 0) {
				throw new IllegalArgumentException("Width must be greater than zero");
			}

			if (height <= 0) {
				throw new IllegalArgumentException("Height must be greater than zero");
			}

			this.width = width;
			this.height = height;

			return this;
		}

		public Builder setPixelFormat(PixelFormat pixelFormat) {
			this.format = pixelFormat;

			return this;
		}

		public Builder setPixelType(PixelType pixelType) {
			this.type = pixelType;

			return this;
		}

		public RenderTarget build() {
			return new RenderTarget(this);
		}
	}
}
