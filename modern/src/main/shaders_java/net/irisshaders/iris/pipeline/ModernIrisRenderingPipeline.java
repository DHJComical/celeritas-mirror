package net.irisshaders.iris.pipeline;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import static com.mitchej123.glsm.GLStateManagerService.GL_STATE_MANAGER;
import static com.mitchej123.glsm.RenderSystemService.RENDER_SYSTEM;
import static org.embeddedt.embeddium.compat.mc.MinecraftVersionShimService.MINECRAFT_SHIM;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import net.irisshaders.iris.IrisCommon;
import net.irisshaders.iris.gl.blending.AlphaTest;
import net.irisshaders.iris.gl.blending.BlendModeOverride;
import net.irisshaders.iris.gl.framebuffer.GlFramebuffer;
import net.irisshaders.iris.gl.image.GlImage;
import net.irisshaders.iris.gl.image.ImageHolder;
import net.irisshaders.iris.gl.program.ComputeProgram;
import net.irisshaders.iris.gl.program.ProgramSamplers;
import net.irisshaders.iris.gl.sampler.SamplerHolder;
import net.irisshaders.iris.gl.state.FogMode;
import net.irisshaders.iris.gl.state.ShaderAttributeInputs;
import net.irisshaders.iris.gl.state.ShaderAttributeInputsBuilder;
import net.irisshaders.iris.gl.texture.DepthBufferFormat;
import net.irisshaders.iris.gui.option.IrisVideoSettings;
import net.irisshaders.iris.helpers.OptionalBoolean;
import net.irisshaders.iris.mixin.GlStateManagerAccessor;
import net.irisshaders.iris.pathways.HorizonRenderer;
import net.irisshaders.iris.pipeline.programs.*;
import net.irisshaders.iris.samplers.IrisImages;
import net.irisshaders.iris.samplers.IrisSamplers;
import net.irisshaders.iris.shaderpack.loading.ProgramId;
import net.irisshaders.iris.shaderpack.materialmap.BlockMaterialMapping;
import net.irisshaders.iris.shaderpack.materialmap.ModelTextureAnalyzer;
import net.irisshaders.iris.shaderpack.materialmap.ModernWorldRenderingSettings;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shaderpack.properties.PackDirectives;
import net.irisshaders.iris.shaderpack.texture.TextureStage;
import net.irisshaders.iris.shadows.CommonShadowRenderer;
import net.irisshaders.iris.shadows.ShadowCompositeRenderer;
import net.irisshaders.iris.shadows.ModernShadowRenderer;
import net.irisshaders.iris.shadows.ShadowRenderTargets;
import net.irisshaders.iris.targets.*;
import net.irisshaders.iris.texture.TextureInfoCache;
import net.irisshaders.iris.texture.format.TextureFormat;
import net.irisshaders.iris.texture.format.TextureFormatLoader;
import net.irisshaders.iris.texture.pbr.PBRTextureHolder;
import net.irisshaders.iris.texture.pbr.PBRTextureManager;
import net.irisshaders.iris.texture.pbr.PBRType;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.irisshaders.iris.uniforms.custom.CustomUniforms;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.GameRenderer;
import org.embeddedt.embeddium.compat.mc.*;
import org.embeddedt.embeddium.impl.gl.debug.GLDebug;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector4f;
import org.lwjgl.opengl.*;

public class ModernIrisRenderingPipeline extends CommonIrisRenderingPipeline implements IrisRenderingPipeline, WorldRenderingPipeline, ShaderRenderingPipeline, RenderTargetStateListener {
    private final HorizonRenderer horizonRenderer = new HorizonRenderer();
	private final int stackSize = 0;

	@Override
    protected void updateMCFBInfo() {
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();

        this.mainFBHeight = main.height;
        this.mainFBWidth = main.width;
        this.mainFBDepthTextureId = main.getDepthTextureId();
        this.mainFBDepthBufferVersion = ((Blaze3dRenderTargetExt) main).iris$getDepthBufferVersion();
    }

    public ModernIrisRenderingPipeline(ProgramSet programSet) {
        super(programSet);
	}

	@Override
	protected boolean checkShadowUsesImages() {
		MCShaderInstance shader = getShaderMap().getShader(ModernShaderKey.SHADOW_TERRAIN_CUTOUT);
		boolean shadowUsesImages = false;

		if (shader instanceof ExtendedShader shader2) {
			shadowUsesImages = shader2.hasActiveImages();
		}

		return shadowUsesImages;
	}

	@Override
	protected void destroyHorizonRenderer() {
		horizonRenderer.destroy();
	}

	@Override
	protected @Nullable CommonShadowRenderer createShadowRenderer(CommonIrisRenderingPipeline commonIrisRenderingPipeline, ProgramSource programSource,
			PackDirectives packDirectives, ShadowRenderTargets shadowRenderTargets, ShadowCompositeRenderer shadowCompositeRenderer,
			CustomUniforms customUniforms, boolean separateHardwareSamplers) {
		return new ModernShadowRenderer(commonIrisRenderingPipeline, programSource, packDirectives, shadowRenderTargets, shadowCompositeRenderer, customUniforms, separateHardwareSamplers);
	}

	@Override
	protected ShaderKey[] getShaderKeyValues() {
		return ModernShaderKey.values();
	}

	@Override
	protected CompletableFuture<MCShaderInstance> createShader(String name, Executor syncExecutor, ProgramSource source, ProgramId programId, AlphaTest fallbackAlpha,
										MCVertexFormat vertexFormat, FogMode fogMode,
										boolean isIntensity, boolean isFullbright, boolean isGlint, boolean isText) throws IOException {
		GlFramebuffer beforeTranslucent = renderTargets.createGbufferFramebuffer(getFlippedAfterPrepare(), source.getDirectives().getDrawBuffers());
		GlFramebuffer afterTranslucent = renderTargets.createGbufferFramebuffer(getFlippedAfterTranslucent(), source.getDirectives().getDrawBuffers());
		boolean isLines = programId == ProgramId.Line && resolver.has(ProgramId.Line);


		ShaderAttributeInputs inputs = new ShaderAttributeInputsBuilder(vertexFormat, isFullbright, isLines, isGlint, isText).build();

		Supplier<ImmutableSet<Integer>> flipped =
			() -> isBeforeTranslucent ? getFlippedAfterPrepare() : getFlippedAfterTranslucent();


        CompletableFuture<ExtendedShader> extendedShaderFuture = ShaderCreator.create(this, syncExecutor, name, source, programId, beforeTranslucent, afterTranslucent,
			fallbackAlpha, vertexFormat, inputs,
				getFrameUpdateNotifier(), this, flipped, fogMode, isIntensity, isFullbright, false, isLines,
				getCustomUniforms());

        return extendedShaderFuture.thenApplyAsync(shader -> {
            loadedShaders.add(shader);
            return shader;
        }, syncExecutor);
	}

	@Override
	protected MCShaderInstance createFallbackShader(String name, ShaderKey key) throws IOException {
		GlFramebuffer beforeTranslucent = renderTargets.createGbufferFramebuffer(getFlippedAfterPrepare(), new int[]{0});
		GlFramebuffer afterTranslucent = renderTargets.createGbufferFramebuffer(getFlippedAfterTranslucent(), new int[]{0});

		FallbackShader shader = ShaderCreator.createFallback(name, beforeTranslucent, afterTranslucent,
			key.getAlphaTest(), key.getVertexFormat(), null, this, key.getFogMode(),
			key == ModernShaderKey.GLINT, key.isText(), key.hasDiffuseLighting(), key.isIntensity(), key.shouldIgnoreLightmap());

		loadedShaders.add(shader);

		return shader;
	}

	@Override
	protected CompletableFuture<MCShaderInstance> createShadowShader(String name, Optional<ProgramSource> source, ShaderKey key, Executor syncExecutor) throws IOException {
		if (!source.isPresent()) {
			return CompletableFuture.completedFuture(createFallbackShadowShader(name, key));
		}

		return createShadowShader(name, syncExecutor, source.get(), key.getProgram(), key.getAlphaTest(), key.getVertexFormat(),
			key.isIntensity(), key.shouldIgnoreLightmap(), key.isText());
	}

	@Override
	protected MCShaderInstance createFallbackShadowShader(String name, ShaderKey key) throws IOException {
		GlFramebuffer framebuffer = shadowRenderTargets.createShadowFramebuffer(ImmutableSet.of(), new int[]{0});

		FallbackShader shader = ShaderCreator.createFallback(name, framebuffer, framebuffer,
			key.getAlphaTest(), key.getVertexFormat(), BlendModeOverride.OFF, this, key.getFogMode(),
			key == ModernShaderKey.GLINT, key.isText(), key.hasDiffuseLighting(), key.isIntensity(), key.shouldIgnoreLightmap());

		loadedShaders.add(shader);

		return shader;
	}

	@Override
	protected CompletableFuture<MCShaderInstance> createShadowShader(String name, Executor syncExecutor, ProgramSource source, ProgramId programId, AlphaTest fallbackAlpha,
											  MCVertexFormat vertexFormat, boolean isIntensity, boolean isFullbright, boolean isText) throws IOException {
		GlFramebuffer framebuffer = shadowRenderTargets.createShadowFramebuffer(ImmutableSet.of(), source.getDirectives().hasUnknownDrawBuffers() ? new int[]{0, 1} : source.getDirectives().getDrawBuffers());
		boolean isLines = programId == ProgramId.Line && resolver.has(ProgramId.Line);

		ShaderAttributeInputs inputs = new ShaderAttributeInputsBuilder(vertexFormat, isFullbright, isLines, false, isText).build();

		Supplier<ImmutableSet<Integer>> flipped = () -> getFlippedBeforeShadow();

		CompletableFuture<ExtendedShader> extendedShaderFuture = ShaderCreator.create(this, syncExecutor, name, source, programId, framebuffer, framebuffer,
			fallbackAlpha, vertexFormat, inputs,
				getFrameUpdateNotifier(), this, flipped, FogMode.PER_VERTEX, isIntensity, isFullbright, true, isLines, getCustomUniforms());

        return extendedShaderFuture.thenApplyAsync(shader -> {
            loadedShaders.add(shader);
            return shader;
        }, syncExecutor);
	}

	public void addGbufferOrShadowSamplers(SamplerHolder samplers, ImageHolder images, Supplier<ImmutableSet<Integer>> flipped,
										   boolean isShadowPass, boolean hasTexture, boolean hasLightmap, boolean hasOverlay) {
		TextureStage textureStage = TextureStage.GBUFFERS_AND_SHADOW;

		ProgramSamplers.CustomTextureSamplerInterceptor samplerHolder =
			ProgramSamplers.customTextureSamplerInterceptor(samplers,
				customTextureManager.getCustomTextureIdMap().getOrDefault(textureStage, Object2ObjectMaps.emptyMap()));

		IrisSamplers.addRenderTargetSamplers(samplerHolder, flipped, renderTargets, false, this);
		IrisSamplers.addCustomTextures(samplerHolder, customTextureManager.getIrisCustomTextures());
		IrisImages.addRenderTargetImages(images, flipped, renderTargets);
		IrisImages.addCustomImages(images, customImages);

		if (!shouldBindPBR) {
			shouldBindPBR = IrisSamplers.hasPBRSamplers(samplerHolder);
		}

		IrisSamplers.addLevelSamplers(samplers, this, (MCAbstractTexture) getWhitePixel(), hasTexture, hasLightmap, hasOverlay);
		IrisSamplers.addWorldDepthSamplers(samplerHolder, this.renderTargets);
		IrisSamplers.addNoiseSampler(samplerHolder, this.customTextureManager.getNoiseTexture());
		IrisSamplers.addCustomImages(samplerHolder, customImages);

		if (IrisSamplers.hasShadowSamplers(samplerHolder)) {
			IrisSamplers.addShadowSamplers(samplerHolder, shadowTargetsSupplier.get(), null, separateHardwareSamplers);
		}

		if (isShadowPass || IrisImages.hasShadowImages(images)) {
			IrisImages.addShadowColorImages(images, shadowTargetsSupplier.get(), null);
		}
	}

    @Override
	public RenderTargetStateListener getRenderTargetStateListener() {
		return this;
	}

    @Override
	public void onSetShaderTexture(int id) {
		if (shouldBindPBR && isRenderingWorld) {
			PBRTextureHolder pbrHolder = PBRTextureManager.INSTANCE.getOrLoadHolder(id);
			currentNormalTexture = pbrHolder.normalTexture().getId();
			currentSpecularTexture = pbrHolder.specularTexture().getId();

			TextureFormat textureFormat = TextureFormatLoader.getFormat();
			if (textureFormat != null) {
				int previousBinding = GlStateManagerAccessor.getTEXTURES()[GlStateManagerAccessor.getActiveTexture()].binding;
				textureFormat.setupTextureParameters(PBRType.NORMAL, pbrHolder.normalTexture());
				textureFormat.setupTextureParameters(PBRType.SPECULAR, pbrHolder.specularTexture());
				GL_STATE_MANAGER.bindTexture(previousBinding);
			}

			PBRTextureManager.notifyPBRTexturesChanged();
		}
	}

	@Override
	public void beginLevelRendering() {
		isRenderingWorld = true;

        if (blockIdsNeedPopulation) {
            ModernWorldRenderingSettings.INSTANCE.setBlockStateIds(
                    BlockMaterialMapping.createBlockStateIdMap(pack.getIdMap().getBlockProperties()));
            ModernWorldRenderingSettings.INSTANCE.setBlockTypeIds(BlockMaterialMapping.createBlockTypeMap(pack.getIdMap().getBlockRenderTypeMap()));
            if (IrisCommon.getIrisConfig().isEnableTextureMaterialFallback()) {
                ModernWorldRenderingSettings.INSTANCE.setFallbackTextureMaterialMapping(ModelTextureAnalyzer.runAnalysisSync(ModernWorldRenderingSettings.INSTANCE.getBlockStateIds()));
            } else {
                ModernWorldRenderingSettings.INSTANCE.setFallbackTextureMaterialMapping(null);
            }
            WorldRenderingSettings.INSTANCE.reloadRendererIfRequired();
            blockIdsNeedPopulation = false;
        }

		// Make sure we're using texture unit 0 for this.
		RENDER_SYSTEM.glActiveTexture(GL15C.GL_TEXTURE0);
		Vector4f emptyClearColor = new Vector4f(1.0F);

        GLDebug.pushGroup(100, "Clear textures");

		for (GlImage image : clearImages) {
			ARBClearTexture.glClearTexImage(image.getId(), 0, image.getFormat().getGlFormat(), image.getPixelType().getGlFormat(), (int[]) null);
		}

		if (hasShadowRenderTargets()) {
			if (packDirectives.getShadowDirectives().isShadowEnabled() == OptionalBoolean.FALSE) {
				if (shadowRenderTargets.isFullClearRequired()) {
					this.shadowClearPasses = ClearPassCreator.createShadowClearPasses(shadowRenderTargets, false, shadowDirectives);
					this.shadowClearPassesFull = ClearPassCreator.createShadowClearPasses(shadowRenderTargets, true, shadowDirectives);
					shadowRenderTargets.onFullClear();
					for (ClearPass clearPass : shadowClearPassesFull) {
						clearPass.execute(emptyClearColor);
					}
				}
			} else {
				// Clear depth first, regardless of any color clearing.
				shadowRenderTargets.getDepthSourceFb().bind();
				RENDER_SYSTEM.clear(GL21C.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);

				ImmutableList<ClearPass> passes;

				for (ComputeProgram computeProgram : shadowComputes) {
					if (computeProgram != null) {
						computeProgram.use();
						getCustomUniforms().push(computeProgram);
						computeProgram.dispatch(shadowMapResolution, shadowMapResolution);
					}
				}

				if (shadowRenderTargets.isFullClearRequired()) {
					this.shadowClearPasses = ClearPassCreator.createShadowClearPasses(shadowRenderTargets, false, shadowDirectives);
					this.shadowClearPassesFull = ClearPassCreator.createShadowClearPasses(shadowRenderTargets, true, shadowDirectives);
					passes = shadowClearPassesFull;
					shadowRenderTargets.onFullClear();
				} else {
					passes = shadowClearPasses;
				}

				for (ClearPass clearPass : passes) {
					clearPass.execute(emptyClearColor);
				}
			}
		}

		// NB: execute this before resizing / clearing so that the center depth sample is retrieved properly.
		getFrameUpdateNotifier().onNewFrame();

		// Update custom uniforms
		this.getCustomUniforms().update();

        this.updateMCFBInfo();

		int internalFormat = TextureInfoCache.INSTANCE.getInfo(mainFBDepthTextureId).getInternalFormat();
		DepthBufferFormat depthBufferFormat = DepthBufferFormat.fromGlEnumOrDefault(internalFormat);

		boolean changed = renderTargets.resizeIfNeeded(
				mainFBDepthBufferVersion, mainFBDepthTextureId, mainFBWidth,
				mainFBHeight, depthBufferFormat, packDirectives);

		if (changed) {
			beginRenderer.recalculateSizes();
			prepareRenderer.recalculateSizes();
			deferredRenderer.recalculateSizes();
			compositeRenderer.recalculateSizes();
			finalPassRenderer.recalculateSwapPassSize();
			if (shaderStorageBufferHolder != null) {
				shaderStorageBufferHolder.hasResizedScreen(mainFBWidth, mainFBHeight);
			}

			customImages.forEach(image -> image.updateNewSize(mainFBWidth, mainFBHeight));

			this.clearPassesFull.forEach(clearPass -> renderTargets.destroyFramebuffer(clearPass.getFramebuffer()));
			this.clearPasses.forEach(clearPass -> renderTargets.destroyFramebuffer(clearPass.getFramebuffer()));

			this.clearPassesFull = ClearPassCreator.createClearPasses(renderTargets, true,
				packDirectives.getRenderTargetDirectives());
			this.clearPasses = ClearPassCreator.createClearPasses(renderTargets, false,
				packDirectives.getRenderTargetDirectives());
		}

		if (changed || IrisVideoSettings.colorSpace != currentColorSpace) {
			currentColorSpace = IrisVideoSettings.colorSpace;
			colorSpaceConverter.rebuildProgram(mainFBWidth, mainFBHeight, currentColorSpace);
		}

		final ImmutableList<ClearPass> passes;

		if (renderTargets.isFullClearRequired()) {
			renderTargets.onFullClear();
			passes = clearPassesFull;
		} else {
			passes = clearPasses;
		}

		Vector3d fogColor3 = CapturedRenderingState.INSTANCE.getFogColor();

		// NB: The alpha value must be 1.0 here, or else you will get a bunch of bugs. Sildur's Vibrant Shaders
		//     will give you pink reflections and other weirdness if this is zero.
		Vector4f fogColor = new Vector4f((float) fogColor3.x, (float) fogColor3.y, (float) fogColor3.z, 1.0F);

		for (ClearPass clearPass : passes) {
			clearPass.execute(fogColor);
		}

        GLDebug.popGroup();

		// Make sure to switch back to the main framebuffer. If we forget to do this then our alt buffers might be
		// cleared to the fog color, which absolutely is not what we want!
		//
		// If we forget to do this, then weird lines appear at the top of the screen and the right of the screen
		// on Sildur's Vibrant Shaders.
        MINECRAFT_SHIM.bindMainFramebuffer();
		setIsMainBound(true);

		if (changed) {
			boolean hasRun = false;

			for (ComputeProgram program : setup) {
				if (program != null) {
					hasRun = true;
					program.use();
					program.dispatch(1, 1);
				}
			}

			if (hasRun) {
				ComputeProgram.unbind();
			}
		}

		isBeforeTranslucent = true;

		beginRenderer.renderAll();

		setPhase(WorldRenderingPhase.SKY);

		// Render our horizon box before actual sky rendering to avoid being broken by mods that do weird things
		// while rendering the sky.
		//
		// A lot of dimension mods touch sky rendering, FabricSkyboxes injects at HEAD and cancels, etc.
		DimensionSpecialEffects.SkyType skyType = Minecraft.getInstance().level.effects().skyType();

		if (skyType == DimensionSpecialEffects.SkyType.NORMAL) {
			RENDER_SYSTEM.depthMask(false);

			RENDER_SYSTEM.setShaderColor(fogColor.x, fogColor.y, fogColor.z, fogColor.w);

			horizonRenderer.renderHorizon(CapturedRenderingState.INSTANCE.getGbufferModelView(), CapturedRenderingState.INSTANCE.getGbufferProjection(), GameRenderer.getPositionShader());

			RENDER_SYSTEM.depthMask(true);

			RENDER_SYSTEM.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		}
	}

	@Override
	public void addDebugText(List<String> messages) {
		if (this.shadowRenderer != null) {
			messages.add("");
			shadowRenderer.addDebugText(messages);
		} else {
			messages.add("");
			messages.add("[Iris] Shadow Maps: not used by shader pack");
		}
	}

	@Override
	public void beginTranslucents() {
		if (destroyed) {
			throw new IllegalStateException("Tried to use a destroyed world rendering pipeline");
		}

        removePhaseIfNeeded();

		isBeforeTranslucent = false;

		// We need to copy the current depth texture so that depthtex1 can contain the depth values for
		// all non-translucent content, as required.
		renderTargets.copyPreTranslucentDepth();

		deferredRenderer.renderAll();

		RENDER_SYSTEM.enableBlend();

		// note: we are careful not to touch the lightmap texture unit or overlay color texture unit here,
		// so we don't need to do anything to restore them if needed.
		//
		// Previous versions of the code tried to "restore" things by enabling the lightmap & overlay color
		// but that actually broke rendering of clouds and rain by making them appear red in the case of
		// a pack not overriding those shader programs.
		//
		// Not good!

		// Reset shader or whatever...
		RenderSystem.setShader(GameRenderer::getPositionShader);
	}

}
