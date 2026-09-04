package net.irisshaders.iris.targets;

import com.google.common.collect.ImmutableSet;
import com.mojang.blaze3d.systems.RenderSystem;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.gl.framebuffer.GlFramebuffer;
import net.irisshaders.iris.gl.texture.DepthBufferFormat;
import net.irisshaders.iris.gl.texture.DepthCopyStrategy;
import net.irisshaders.iris.shaderpack.properties.PackDirectives;
import net.irisshaders.iris.shaderpack.properties.PackRenderTargetDirectives;
import org.joml.Vector2i;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class RenderTargets {
	private final RenderTarget[] targets;
	private final DepthTexture noTranslucents;
	private final DepthTexture noHand;
	private final GlFramebuffer depthSourceFb;
	private final GlFramebuffer noTranslucentsDestFb;
	private final GlFramebuffer noHandDestFb;
	private final List<GlFramebuffer> ownedFramebuffers;
	private final List<FramebufferSpec> framebufferSpecs;
	private final Map<Integer, PackRenderTargetDirectives.RenderTargetSettings> targetSettingsMap;
	private final PackDirectives packDirectives;
	private int currentDepthTexture;
	private DepthBufferFormat currentDepthFormat;
	private DepthCopyStrategy copyStrategy;
	private int cachedWidth;
	private int cachedHeight;
	private boolean fullClearRequired;
	private boolean translucentDepthDirty;
	private boolean handDepthDirty;
	private boolean preHandDepthUsed;
	private boolean oddParity;
	private ImmutableSet<Integer> altUsedTargets = ImmutableSet.of();

	private int cachedDepthBufferVersion;
	private boolean destroyed;

	public RenderTargets(int width, int height, int depthTexture, int depthBufferVersion, DepthBufferFormat depthFormat, Map<Integer, PackRenderTargetDirectives.RenderTargetSettings> renderTargets, PackDirectives packDirectives) {
		targets = new RenderTarget[renderTargets.size()];

		targetSettingsMap = renderTargets;
		this.packDirectives = packDirectives;

		this.currentDepthTexture = depthTexture;
		this.currentDepthFormat = depthFormat;
		this.copyStrategy = DepthCopyStrategy.fastest(currentDepthFormat.isCombinedStencil());

		this.cachedWidth = width;
		this.cachedHeight = height;
		this.cachedDepthBufferVersion = depthBufferVersion;

		this.ownedFramebuffers = new ArrayList<>();
		this.framebufferSpecs = new ArrayList<>();

		// NB: Make sure all buffers are cleared so that they don't contain undefined
		// data. Otherwise very weird things can happen.
		fullClearRequired = true;

		this.depthSourceFb = createFramebufferWritingToMain(new int[]{0});

		this.noTranslucents = new DepthTexture("depthtex1", width, height, currentDepthFormat);
		this.noHand = new DepthTexture("dephtex2", width, height, currentDepthFormat);

		this.noTranslucentsDestFb = createFramebufferWritingToMain(new int[]{0});
		this.noTranslucentsDestFb.addDepthAttachment(this.noTranslucents.getTextureId());

		this.noHandDestFb = createFramebufferWritingToMain(new int[]{0});
		this.noHandDestFb.addDepthAttachment(this.noHand.getTextureId());

		this.translucentDepthDirty = true;
		this.handDepthDirty = true;
	}

	public void destroy() {
		destroyed = true;

		for (GlFramebuffer owned : ownedFramebuffers) {
			owned.delete();
		}

		for (RenderTarget target : targets) {
			if (target != null) {
				target.destroy();
			}
		}

		noTranslucents.delete();
		noHand.delete();
	}

	public int getRenderTargetCount() {
		return targets.length;
	}

	public RenderTarget get(int index) {
		if (destroyed) {
			throw new IllegalStateException("Tried to use destroyed RenderTargets");
		}

		if (targets[index] == null) {
			return null;
		}

		return targets[index];
	}

	public RenderTarget getOrCreate(int index) {
		if (destroyed) {
			throw new IllegalStateException("Tried to use destroyed RenderTargets");
		}

		if (targets[index] != null) return targets[index];

		create(index);

		return targets[index];
	}

	private void create(int index) {
		PackRenderTargetDirectives.RenderTargetSettings settings = targetSettingsMap.get(index);
		Vector2i dimensions = packDirectives.getTextureScaleOverride(index, cachedWidth, cachedHeight);
		targets[index] = RenderTarget.builder().setDimensions(dimensions.x, dimensions.y)
			.setName("colortex" + index)
			.setInternalFormat(settings.getInternalFormat())
			.setPixelFormat(settings.getInternalFormat().getPixelFormat()).build();
	}

	public void declareAltUsed(ImmutableSet<Integer> altUsedTargets) {
		this.altUsedTargets = altUsedTargets;
	}

	public boolean isAltUsed(int index) {
		return altUsedTargets.contains(index);
	}

	public void setPreHandDepthUsed() {
		this.preHandDepthUsed = true;
	}

	public void enableParity(ImmutableSet<Integer> parityTargets) {
		for (int target : parityTargets) {
			getOrCreate(target).enableParity(this::isOddParity);
		}

		for (FramebufferSpec spec : framebufferSpecs) {
			buildOddParityVariant(spec);
		}
	}

	public void advanceParity() {
		oddParity = !oddParity;
	}

	public boolean isOddParity() {
		return oddParity;
	}

	private void buildOddParityVariant(FramebufferSpec spec) {
		if (spec.framebuffer.hasOddVariant()) {
			return;
		}

		boolean needed = false;

		for (int buffer : spec.drawBuffers) {
			if (targets[buffer].isParityEnabled()) {
				needed = true;
				break;
			}
		}

		if (!needed) {
			return;
		}

		configure(spec.framebuffer().createOddVariant(), spec, true);
	}

	private record FramebufferSpec(ParityFramebuffer framebuffer, ImmutableSet<Integer> stageWritesToMain,
								   int[] drawBuffers) {
	}

	public int getDepthTexture() {
		return currentDepthTexture;
	}

	public DepthTexture getDepthTextureNoTranslucents() {
		if (destroyed) {
			throw new IllegalStateException("Tried to use destroyed RenderTargets");
		}

		return noTranslucents;
	}

	public DepthTexture getDepthTextureNoHand() {
		return noHand;
	}

	public boolean resizeIfNeeded(int newDepthBufferVersion, int newDepthTextureId, int newWidth, int newHeight, DepthBufferFormat newDepthFormat, PackDirectives packDirectives) {
		boolean recreateDepth = false;
		if (cachedDepthBufferVersion != newDepthBufferVersion || currentDepthTexture != newDepthTextureId) {
			recreateDepth = true;
			currentDepthTexture = newDepthTextureId;
			cachedDepthBufferVersion = newDepthBufferVersion;
		}

		boolean sizeChanged = newWidth != cachedWidth || newHeight != cachedHeight;
		boolean depthFormatChanged = newDepthFormat != currentDepthFormat;

		if (depthFormatChanged) {
			currentDepthFormat = newDepthFormat;
			// Might need a new copy strategy
			copyStrategy = DepthCopyStrategy.fastest(currentDepthFormat.isCombinedStencil());
		}

		if (recreateDepth) {
			// Re-attach the depth textures with the new depth texture ID, since Minecraft re-creates
			// the depth texture when resizing its render targets.
			//
			// I'm not sure if our framebuffers holding on to the old depth texture between frames
			// could be a concern, in the case of resizing and similar. I think it should work
			// based on what I've seen of the spec, though - it seems like deleting a texture
			// automatically detaches it from its framebuffers.
			for (GlFramebuffer framebuffer : ownedFramebuffers) {
				if (framebuffer == noHandDestFb || framebuffer == noTranslucentsDestFb) {
					// NB: Do not change the depth attachment of these framebuffers
					// as it is intentionally different
					continue;
				}

				if (framebuffer.hasDepthAttachment()) {
					framebuffer.addDepthAttachment(newDepthTextureId);
				}
			}
		}

		if (depthFormatChanged || sizeChanged) {
			// Reallocate depth buffers
			noTranslucents.resize(newWidth, newHeight, newDepthFormat);
			noHand.resize(newWidth, newHeight, newDepthFormat);
			this.translucentDepthDirty = true;
			this.handDepthDirty = true;
		}

		if (sizeChanged) {
			cachedWidth = newWidth;
			cachedHeight = newHeight;

			for (int i = 0; i < targets.length; i++) {
				if (targets[i] != null) {
					targets[i].resize(packDirectives.getTextureScaleOverride(i, newWidth, newHeight));
				}
			}

			fullClearRequired = true;
		}

		return sizeChanged;
	}

	public void copyPreTranslucentDepth() {
		if (translucentDepthDirty) {
			translucentDepthDirty = false;
			RenderSystem.bindTexture(noTranslucents.getTextureId());
			depthSourceFb.bindAsReadBuffer();
			IrisRenderSystem.copyTexImage2D(GL20C.GL_TEXTURE_2D, 0, currentDepthFormat.getGlInternalFormat(), 0, 0, cachedWidth, cachedHeight, 0);
		} else {
			copyStrategy.copy(depthSourceFb, getDepthTexture(), noTranslucentsDestFb, noTranslucents.getTextureId(),
				getCurrentWidth(), getCurrentHeight());
		}
	}

	public void copyPreHandDepth() {
		if (!preHandDepthUsed) {
			// Nothing reads depthtex2, so there is no reason to spend a full-resolution depth copy on it.
			return;
		}

		if (handDepthDirty) {
			handDepthDirty = false;
			RenderSystem.bindTexture(noHand.getTextureId());
			depthSourceFb.bindAsReadBuffer();
			IrisRenderSystem.copyTexImage2D(GL20C.GL_TEXTURE_2D, 0, currentDepthFormat.getGlInternalFormat(), 0, 0, cachedWidth, cachedHeight, 0);
		} else {
			copyStrategy.copy(depthSourceFb, getDepthTexture(), noHandDestFb, noHand.getTextureId(),
				getCurrentWidth(), getCurrentHeight());
		}
	}

	public boolean isFullClearRequired() {
		return fullClearRequired;
	}

	public void onFullClear() {
		fullClearRequired = false;
	}

	public GlFramebuffer createFramebufferWritingToMain(int[] drawBuffers) {
		return createFullFramebuffer(false, drawBuffers);
	}

	public GlFramebuffer createFramebufferWritingToAlt(int[] drawBuffers) {
		return createFullFramebuffer(true, drawBuffers);
	}

	public GlFramebuffer createClearFramebuffer(boolean alt, int[] clearBuffers) {
		ImmutableSet<Integer> stageWritesToMain = ImmutableSet.of();

		if (!alt) {
			stageWritesToMain = invert(ImmutableSet.of(), clearBuffers);
		}

		return createColorFramebuffer(stageWritesToMain, clearBuffers);
	}

	private ImmutableSet<Integer> invert(ImmutableSet<Integer> base, int[] relevant) {
		ImmutableSet.Builder<Integer> inverted = ImmutableSet.builder();

		for (int i : relevant) {
			if (!base.contains(i)) {
				inverted.add(i);
			}
		}

		return inverted.build();
	}

	private GlFramebuffer createEmptyFramebuffer() {
		GlFramebuffer framebuffer = new GlFramebuffer();
		ownedFramebuffers.add(framebuffer);

		framebuffer.addDepthAttachment(currentDepthTexture);

		// NB: Before OpenGL 3.0, all framebuffers are required to have a color
		// attachment no matter what.
		framebuffer.addColorAttachment(0, getOrCreate(0).getMainTexture());
		framebuffer.noDrawBuffers();

		return framebuffer;
	}

	public GlFramebuffer createDHFramebuffer(ImmutableSet<Integer> stageWritesToAlt, int[] drawBuffers) {
		if (drawBuffers.length == 0) {
			return createEmptyFramebuffer();
		}

		ImmutableSet<Integer> stageWritesToMain = invert(stageWritesToAlt, drawBuffers);

		GlFramebuffer framebuffer = createColorFramebuffer(stageWritesToMain, drawBuffers);

		return framebuffer;
	}


	public GlFramebuffer createGbufferFramebuffer(ImmutableSet<Integer> stageWritesToAlt, int[] drawBuffers) {
		if (drawBuffers.length == 0) {
			return createEmptyFramebuffer();
		}

		ImmutableSet<Integer> stageWritesToMain = invert(stageWritesToAlt, drawBuffers);

		GlFramebuffer framebuffer = createColorFramebuffer(stageWritesToMain, drawBuffers);

		framebuffer.addDepthAttachment(currentDepthTexture);

		return framebuffer;
	}

	private GlFramebuffer createFullFramebuffer(boolean clearsAlt, int[] drawBuffers) {
		if (drawBuffers.length == 0) {
			return createEmptyFramebuffer();
		}

		ImmutableSet<Integer> stageWritesToMain = ImmutableSet.of();

		if (!clearsAlt) {
			stageWritesToMain = invert(ImmutableSet.of(), drawBuffers);
		}

		return createColorFramebufferWithDepth(stageWritesToMain, drawBuffers);
	}

	public GlFramebuffer createColorFramebufferWithDepth(ImmutableSet<Integer> stageWritesToMain, int[] drawBuffers) {
		GlFramebuffer framebuffer = createColorFramebuffer(stageWritesToMain, drawBuffers);

		framebuffer.addDepthAttachment(currentDepthTexture);

		return framebuffer;
	}

	public GlFramebuffer createColorFramebuffer(ImmutableSet<Integer> stageWritesToMain, int[] drawBuffers) {
		if (drawBuffers.length == 0) {
			throw new IllegalArgumentException("Framebuffer must have at least one color buffer");
		}

		ParityFramebuffer framebuffer = new ParityFramebuffer(this::isOddParity);
		ownedFramebuffers.add(framebuffer);

		for (int drawBuffer : drawBuffers) {
			if (drawBuffer >= getRenderTargetCount()) {
				// TODO: This causes resource leaks, also we should really verify this in the shaderpack parser...
				framebuffer.delete();
				ownedFramebuffers.remove(framebuffer);
				throw new IllegalStateException("Render target with index " + drawBuffer + " is not supported, only "
					+ getRenderTargetCount() + " render targets are supported.");
			}

			this.getOrCreate(drawBuffer);
		}

		FramebufferSpec spec = new FramebufferSpec(framebuffer, stageWritesToMain, drawBuffers.clone());

		configure(framebuffer, spec, false);

		framebufferSpecs.add(spec);
		buildOddParityVariant(spec);

		int status = framebuffer.getStatus();
		if (status != GL30C.GL_FRAMEBUFFER_COMPLETE) {
			throw new IllegalStateException("Unexpected error while creating framebuffer: Draw buffers " + Arrays.toString(drawBuffers) + " Status: " + status);
		}

		return framebuffer;
	}

	/**
	 * Attaches everything described by the spec, resolved for the given frame parity. Both variants of a parity
	 * framebuffer go through here, so they cannot drift apart.
	 */
	private void configure(GlFramebuffer framebuffer, FramebufferSpec spec, boolean odd) {
		int[] actualDrawBuffers = new int[spec.drawBuffers().length];

		for (int i = 0; i < spec.drawBuffers().length; i++) {
			int buffer = spec.drawBuffers()[i];
			actualDrawBuffers[i] = i;

			framebuffer.addColorAttachment(i, targets[buffer].getTextureForParity(spec.stageWritesToMain().contains(buffer), odd));
		}

		framebuffer.drawBuffers(actualDrawBuffers);
		framebuffer.readBuffer(0);

		// A depth attachment is added by the caller after creation, so on the even variant there is nothing to do
		// here; the odd variant is built later and has to pick up whatever the even one ended up with.
		if (framebuffer != spec.framebuffer() && spec.framebuffer().hasDepthAttachment()) {
			framebuffer.addDepthAttachment(spec.framebuffer().getDepthAttachment());
		}
	}

	public void destroyFramebuffer(GlFramebuffer framebuffer) {
		framebuffer.delete();
		ownedFramebuffers.remove(framebuffer);
		framebufferSpecs.removeIf(spec -> spec.framebuffer() == framebuffer);
	}

	public int getCurrentWidth() {
		return cachedWidth;
	}

	public int getCurrentHeight() {
		return cachedHeight;
	}

	public void createIfUnsure(int index) {
		if (targets[index] == null) {
			create(index);
		}
	}
}
