package net.irisshaders.iris.pipeline;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import net.irisshaders.iris.compat.dh.DHCompat;
import net.irisshaders.iris.features.FeatureFlags;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.gl.blending.AlphaTest;
import net.irisshaders.iris.gl.buffer.ShaderStorageBufferHolder;
import net.irisshaders.iris.gl.framebuffer.GlFramebuffer;
import net.irisshaders.iris.gl.image.GlImage;
import net.irisshaders.iris.gl.image.ImageHolder;
import net.irisshaders.iris.gl.program.ComputeProgram;
import net.irisshaders.iris.gl.program.ProgramBuilder;
import net.irisshaders.iris.gl.program.ProgramImages;
import net.irisshaders.iris.gl.program.ProgramSamplers;
import net.irisshaders.iris.gl.sampler.SamplerHolder;
import net.irisshaders.iris.gl.sampler.SamplerLimits;
import net.irisshaders.iris.gl.shader.ShaderCompileException;
import net.irisshaders.iris.gl.state.FogMode;
import net.irisshaders.iris.gl.texture.DepthBufferFormat;
import net.irisshaders.iris.gl.texture.TextureType;
import net.irisshaders.iris.gui.option.IrisVideoSettings;
import net.irisshaders.iris.helpers.OptionalBoolean;
import net.irisshaders.iris.helpers.Tri;
import net.irisshaders.iris.pathways.CenterDepthSampler;
import net.irisshaders.iris.pathways.colorspace.ColorSpace;
import net.irisshaders.iris.pathways.colorspace.ColorSpaceConverter;
import net.irisshaders.iris.pathways.colorspace.ColorSpaceFragmentConverter;
import net.irisshaders.iris.pipeline.foss_transform.TransformPatcherBridge;
import net.irisshaders.iris.pipeline.programs.ShaderKey;
import net.irisshaders.iris.pipeline.programs.ShaderMap;
import net.irisshaders.iris.pipeline.transform.ShaderPrinter;
import net.irisshaders.iris.samplers.IrisImages;
import net.irisshaders.iris.samplers.IrisSamplers;
import net.irisshaders.iris.shaderpack.FilledIndirectPointer;
import net.irisshaders.iris.shaderpack.ImageInformation;
import net.irisshaders.iris.shaderpack.ShaderPack;
import net.irisshaders.iris.shaderpack.loading.ProgramId;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.irisshaders.iris.shaderpack.programs.ComputeSource;
import net.irisshaders.iris.shaderpack.programs.ProgramFallbackResolver;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shaderpack.properties.CloudSetting;
import net.irisshaders.iris.shaderpack.properties.PackDirectives;
import net.irisshaders.iris.shaderpack.properties.PackShadowDirectives;
import net.irisshaders.iris.shaderpack.properties.ParticleRenderingSettings;
import net.irisshaders.iris.shaderpack.texture.TextureStage;
import net.irisshaders.iris.shadows.CommonShadowRenderer;
import net.irisshaders.iris.shadows.ShadowCompositeRenderer;
import net.irisshaders.iris.shadows.ShadowRenderTargets;
import net.irisshaders.iris.shadows.ShadowRenderingState;
import net.irisshaders.iris.targets.BufferFlipper;
import net.irisshaders.iris.targets.ClearPass;
import net.irisshaders.iris.targets.ClearPassCreator;
import net.irisshaders.iris.targets.RenderTargetStateListener;
import net.irisshaders.iris.targets.RenderTargets;
import net.irisshaders.iris.targets.backed.NativeImageBackedSingleColorTexture;
import net.irisshaders.iris.texture.TextureInfoCache;
import net.irisshaders.iris.texture.format.TextureFormat;
import net.irisshaders.iris.texture.format.TextureFormatLoader;
import net.irisshaders.iris.texture.pbr.PBRTextureHolder;
import net.irisshaders.iris.texture.pbr.PBRTextureManager;
import net.irisshaders.iris.texture.pbr.PBRType;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.irisshaders.iris.uniforms.CommonUniforms;
import net.irisshaders.iris.uniforms.FrameUpdateNotifier;
import net.irisshaders.iris.uniforms.custom.CustomUniforms;
import org.apache.commons.lang3.StringUtils;
import org.embeddedt.embeddium.compat.mc.MCAbstractTexture;
import org.embeddedt.embeddium.compat.mc.MCCamera;
import org.embeddedt.embeddium.compat.mc.MCDynamicTexture;
import org.embeddedt.embeddium.compat.mc.MCLevelRenderer;
import org.embeddedt.embeddium.compat.mc.MCShaderInstance;
import org.embeddedt.embeddium.compat.mc.MCVertexFormat;
import org.embeddedt.embeddium.impl.gl.debug.GLDebug;
import org.embeddedt.embeddium.impl.gl.shader.ShaderType;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector4f;
import org.lwjgl.opengl.ARBClearTexture;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL21C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL43C;

import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import static com.mitchej123.glsm.GLStateManagerService.GL_STATE_MANAGER;
import static com.mitchej123.glsm.RenderSystemService.RENDER_SYSTEM;
import static net.irisshaders.iris.IrisLogging.IRIS_LOGGER;
import static org.embeddedt.embeddium.compat.mc.MinecraftVersionShimService.MINECRAFT_SHIM;

public abstract class CommonIrisRenderingPipeline implements WorldRenderingPipeline, IrisRenderingPipeline, ShaderRenderingPipeline, RenderTargetStateListener {

    protected boolean blockIdsNeedPopulation;
    protected final ColorSpaceConverter colorSpaceConverter;
    protected final ComputeProgram[] setup;
    protected final SodiumTerrainPipeline sodiumTerrainPipeline;
    protected final ShadowCompositeRenderer shadowCompositeRenderer;
    protected final ShaderMap shaderMap;
    protected final ImmutableSet<Integer> flippedBeforeShadow;
    protected final ImmutableSet<Integer> flippedAfterPrepare;
    protected final ImmutableSet<Integer> flippedAfterTranslucent;
    public boolean isBeforeTranslucent;
    protected final Supplier<ShadowRenderTargets> shadowTargetsSupplier;
    protected final int shadowMapResolution;
    protected final PackShadowDirectives shadowDirectives;
    protected final PackDirectives packDirectives;
    protected final Set<GlImage> customImages;
    protected final GlImage[] clearImages;
    protected final ShaderPack pack;
    protected final FrameUpdateNotifier updateNotifier;
    protected final CustomUniforms customUniforms;
    protected final float sunPathRotation;
    protected final boolean shouldRenderUnderwaterOverlay;
    protected final boolean shouldRenderVignette;
    protected final boolean shouldWriteRainAndSnowToDepthBuffer;
    protected final boolean oldLighting;
    protected final OptionalInt forcedShadowRenderDistanceChunks;
    protected final boolean frustumCulling;
    protected final boolean occlusionCulling;
    protected final boolean shouldRenderSun;
    protected final boolean shouldRenderMoon;
    protected final boolean allowConcurrentCompute;
    protected final CloudSetting cloudSetting;
    protected final Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> customTextureMap;
    protected final boolean separateHardwareSamplers;
    protected final ProgramFallbackResolver resolver;
    protected final ParticleRenderingSettings particleRenderingSettings;
    protected final RenderTargets renderTargets;
    protected final CenterDepthSampler centerDepthSampler;
    protected final CustomTextureManager customTextureManager;

    @Nullable
    protected final CommonShadowRenderer shadowRenderer;

    protected final CompositeRenderer beginRenderer;
    protected final CompositeRenderer prepareRenderer;
    protected final CompositeRenderer deferredRenderer;
    protected final CompositeRenderer compositeRenderer;
    protected final FinalPassRenderer finalPassRenderer;
    protected final MCDynamicTexture whitePixel;
    @Nullable
    protected final ComputeProgram[] shadowComputes;
    protected final DHCompat dhCompat;
    protected final Set<MCShaderInstance> loadedShaders;

    protected ShadowRenderTargets shadowRenderTargets;
    protected ImmutableList<ClearPass> clearPassesFull;
    protected ImmutableList<ClearPass> clearPasses;
    protected ImmutableList<ClearPass> shadowClearPasses;
    protected ImmutableList<ClearPass> shadowClearPassesFull;
    protected GlFramebuffer defaultFB;
    protected GlFramebuffer defaultFBAlt;
    protected GlFramebuffer defaultFBShadow;
    protected boolean shouldRemovePhase = false;
    protected ShaderStorageBufferHolder shaderStorageBufferHolder;
    protected WorldRenderingPhase overridePhase = null;
    protected WorldRenderingPhase phase = WorldRenderingPhase.NONE;
    protected boolean destroyed = false;
    protected boolean isRenderingWorld;
    protected boolean isMainBound;
    protected boolean shouldBindPBR;
    protected int currentNormalTexture;
    protected int currentSpecularTexture;
    protected ColorSpace currentColorSpace;
    protected CloudSetting dhCloudSetting;
    protected int mainFBWidth;
    protected int mainFBHeight;
    protected int mainFBDepthTextureId;
    protected int mainFBDepthBufferVersion;


    @Override
    public boolean shouldRenderVignette() {
        return shouldRenderVignette;
    }

    @Override
    public boolean shouldRenderUnderwaterOverlay() {
        return shouldRenderUnderwaterOverlay;
    }

    @Override
    public boolean shouldRenderSun() {
        return shouldRenderSun;
    }

    @Override
    public boolean shouldRenderMoon() {
        return shouldRenderMoon;
    }

    @Override
    public boolean shouldWriteRainAndSnowToDepthBuffer() {
        return shouldWriteRainAndSnowToDepthBuffer;
    }

    @Override
    public ParticleRenderingSettings getParticleRenderingSettings() {
        return particleRenderingSettings;
    }

    @Override
    public boolean allowConcurrentCompute() {
        return allowConcurrentCompute;
    }

    @Override
    public boolean hasFeature(FeatureFlags flag) {
        return pack.hasFeature(flag);
    }

    @Override
    public boolean shouldDisableDirectionalShading() {
        return !oldLighting;
    }

    @Override
    public void renderShadows(MCLevelRenderer worldRenderer, MCCamera playerCamera) {
        if (shadowRenderer != null) {
            this.shadowRenderer.renderShadows(worldRenderer, playerCamera);
        }

        prepareRenderer.renderAll();
    }

    @Override
    public void beginHand() {
        centerDepthSampler.sampleCenterDepth();

        // We need to copy the current depth texture so that depthtex2 can contain the depth values for
        // all non-translucent content excluding the hand, as required.
        renderTargets.copyPreHandDepth();
    }

    @Override
    public void finalizeLevelRendering() {
        isRenderingWorld = false;
        removePhaseIfNeeded();
        compositeRenderer.renderAll();
        finalPassRenderer.renderFinalPass();
    }

    @Override
    public void finalizeGameRendering() {
        colorSpaceConverter.process(MINECRAFT_SHIM.getColorTextureId());
    }

    @Override
    public boolean shouldDisableVanillaEntityShadows() {
        // OptiFine seems to disable vanilla shadows when the shaderpack uses shadow mapping?
        return shadowRenderer != null;
    }

    @Override
    public boolean shouldDisableFrustumCulling() {
        return !frustumCulling;
    }

    @Override
    public boolean shouldDisableOcclusionCulling() {
        return !occlusionCulling;
    }

    @Override
    public CloudSetting getCloudSetting() {
        return cloudSetting;
    }

    @Override
    public OptionalInt getForcedShadowRenderDistanceChunksForDisplay() {
        return forcedShadowRenderDistanceChunks;
    }

    @Override
    public ShaderMap getShaderMap() {
        return shaderMap;
    }

    @Override
    public boolean shouldOverrideShaders() {
        return isRenderingWorld && isMainBound;
    }

    @Override
    public SodiumTerrainPipeline getSodiumTerrainPipeline() {
        return sodiumTerrainPipeline;
    }

    @Override
    public FrameUpdateNotifier getFrameUpdateNotifier() {
        return updateNotifier;
    }

    @Override
    public float getSunPathRotation() {
        return sunPathRotation;
    }

    @Override
    public DHCompat getDHCompat() {
        return dhCompat;
    }

    @Override
    public void setIsMainBound(boolean bound) {
        isMainBound = bound;
    }

    public Optional<ProgramSource> getDHTerrainShader() {
        return resolver.resolve(ProgramId.DhTerrain);
    }

    public Optional<ProgramSource> getDHGenericShader() {
        return resolver.resolve(ProgramId.DhGeneric);
    }

    public Optional<ProgramSource> getDHWaterShader() {
        return resolver.resolve(ProgramId.DhWater);
    }

    public CloudSetting getDHCloudSetting() {
        return dhCloudSetting;
    }

    public void bindDefault() {
        if (isBeforeTranslucent) {
            defaultFB.bind();
        } else {
            defaultFBAlt.bind();
        }
    }

    public void bindDefaultShadow() {
        defaultFBShadow.bind();
    }

    public Optional<ProgramSource> getDHShadowShader() {
        return resolver.resolve(ProgramId.DhShadow);
    }

    public CustomUniforms getCustomUniforms() {
        return customUniforms;
    }

    public GlFramebuffer createDHFramebuffer(ProgramSource sources, boolean trans) {
        return renderTargets.createDHFramebuffer(trans ? flippedAfterTranslucent : flippedAfterPrepare, sources.getDirectives().getDrawBuffers());
    }

    public ImmutableSet<Integer> getFlippedBeforeShadow() {
        return flippedBeforeShadow;
    }

    public ImmutableSet<Integer> getFlippedAfterPrepare() {
        return flippedAfterPrepare;
    }

    public ImmutableSet<Integer> getFlippedAfterTranslucent() {
        return flippedAfterTranslucent;
    }

    public GlFramebuffer createDHFramebufferShadow(ProgramSource sources) {

        return shadowRenderTargets.createDHFramebuffer(ImmutableSet.of(), new int[] { 0, 1 });
    }

    public boolean hasShadowRenderTargets() {
        return shadowRenderTargets != null;
    }

    @Override
    public void onSetShaderTexture(int id) {
        if (shouldBindPBR && isRenderingWorld) {
            PBRTextureHolder pbrHolder = PBRTextureManager.INSTANCE.getOrLoadHolder(id);
            currentNormalTexture = pbrHolder.normalTexture().getId();
            currentSpecularTexture = pbrHolder.specularTexture().getId();

            TextureFormat textureFormat = TextureFormatLoader.getFormat();
            if (textureFormat != null) {
                int previousBinding = GL_STATE_MANAGER.getActiveBoundTexture();
                textureFormat.setupTextureParameters(PBRType.NORMAL, pbrHolder.normalTexture());
                textureFormat.setupTextureParameters(PBRType.SPECULAR, pbrHolder.specularTexture());
                GL_STATE_MANAGER.bindTexture(previousBinding);
            }

            PBRTextureManager.notifyPBRTexturesChanged();
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
        RENDER_SYSTEM.setPositionShader();
    }

    @Override
    public void beginLevelRendering() {
        isRenderingWorld = true;

        if (blockIdsNeedPopulation) {
            MINECRAFT_SHIM.populateBlockIds(pack);

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
                RENDER_SYSTEM.clear(GL21C.GL_DEPTH_BUFFER_BIT, MINECRAFT_SHIM.isOnOSX());

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

        boolean changed = renderTargets.resizeIfNeeded(mainFBDepthBufferVersion, mainFBDepthTextureId, mainFBWidth, mainFBHeight, depthBufferFormat, packDirectives);

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

            this.clearPassesFull = ClearPassCreator.createClearPasses(renderTargets, true, packDirectives.getRenderTargetDirectives());
            this.clearPasses = ClearPassCreator.createClearPasses(renderTargets, false, packDirectives.getRenderTargetDirectives());
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

        if (MINECRAFT_SHIM.isSkyTypeNormal()) {
            RENDER_SYSTEM.depthMask(false);

            RENDER_SYSTEM.setShaderColor(fogColor.x, fogColor.y, fogColor.z, fogColor.w);

            renderHorizon();

            RENDER_SYSTEM.depthMask(true);

            RENDER_SYSTEM.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
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

    protected abstract void renderHorizon();

    /**
     * Updates information about the main FB
     *   - Be sure to call this before using the MC FB info
     */
    protected abstract void updateMCFBInfo();

    public CommonIrisRenderingPipeline(ProgramSet programSet) {
        ShaderPrinter.resetPrintState();
        updateMCFBInfo();

        this.shouldRenderUnderwaterOverlay = programSet.getPackDirectives().underwaterOverlay();
        this.shouldRenderVignette = programSet.getPackDirectives().vignette();
        this.shouldWriteRainAndSnowToDepthBuffer = programSet.getPackDirectives().rainDepth();
        this.oldLighting = programSet.getPackDirectives().isOldLighting();
        this.updateNotifier = new FrameUpdateNotifier();
        this.packDirectives = programSet.getPackDirectives();
        this.customTextureMap = programSet.getPackDirectives().getTextureMap();
        this.separateHardwareSamplers = programSet.getPack().hasFeature(FeatureFlags.SEPARATE_HARDWARE_SAMPLERS);
        this.shadowDirectives = packDirectives.getShadowDirectives();
        this.cloudSetting = programSet.getPackDirectives().getCloudSetting();
        this.dhCloudSetting = programSet.getPackDirectives().getDHCloudSetting();
        this.shouldRenderSun = programSet.getPackDirectives().shouldRenderSun();
        this.shouldRenderMoon = programSet.getPackDirectives().shouldRenderMoon();
        this.allowConcurrentCompute = programSet.getPackDirectives().getConcurrentCompute();
        this.frustumCulling = programSet.getPackDirectives().shouldUseFrustumCulling();
        this.occlusionCulling = programSet.getPackDirectives().shouldUseOcclusionCulling();
        this.resolver = new ProgramFallbackResolver(programSet);
        this.pack = programSet.getPack();

        int internalFormat = TextureInfoCache.INSTANCE.getInfo(mainFBDepthTextureId).getInternalFormat();
        DepthBufferFormat depthBufferFormat = DepthBufferFormat.fromGlEnumOrDefault(internalFormat);

        if (!programSet.getPackDirectives().getBufferObjects().isEmpty()) {
            if (IrisRenderSystem.supportsSSBO()) {
                this.shaderStorageBufferHolder = new ShaderStorageBufferHolder(programSet.getPackDirectives().getBufferObjects(), mainFBWidth, mainFBHeight);

                this.shaderStorageBufferHolder.setupBuffers();
            } else {
                throw new IllegalStateException(
                        "Shader storage buffers/immutable buffer storage is not supported on this graphics card, however the shaderpack requested them? This shouldn't be possible.");
            }
        } else {
            for (int i = 0; i < Math.min(16, SamplerLimits.get().getMaxShaderStorageUnits()); i++) {
                IrisRenderSystem.bindBufferBase(GL43C.GL_SHADER_STORAGE_BUFFER, i, 0);
            }
        }

        this.customImages = new HashSet<>();
        for (ImageInformation information : programSet.getPack().getIrisCustomImages()) {
            if (information.isRelative()) {
                customImages.add(new GlImage.Relative(
                        information.name(),
                        information.samplerName(),
                        information.format(),
                        information.internalTextureFormat(),
                        information.type(),
                        information.clear(),
                        information.relativeWidth(),
                        information.relativeHeight(),
                        mainFBWidth,
                        mainFBHeight));
            } else {
                customImages.add(new GlImage(
                        information.name(),
                        information.samplerName(),
                        information.target(),
                        information.format(),
                        information.internalTextureFormat(),
                        information.type(),
                        information.clear(),
                        information.width(),
                        information.height(),
                        information.depth()));
            }
        }
        this.clearImages = customImages.stream().filter(GlImage::shouldClear).toArray(GlImage[]::new);

        this.particleRenderingSettings = programSet.getPackDirectives().getParticleRenderingSettings().orElseGet(() -> {
            if (programSet.getDeferred().length > 0 && !programSet.getPackDirectives().shouldUseSeparateEntityDraws()) {
                return ParticleRenderingSettings.AFTER;
            } else {
                return ParticleRenderingSettings.MIXED;
            }
        });

        this.renderTargets = new RenderTargets(
                mainFBWidth,
                mainFBHeight,
                mainFBDepthTextureId,
                mainFBDepthBufferVersion,
                depthBufferFormat,
                programSet.getPackDirectives().getRenderTargetDirectives().getRenderTargetSettings(),
                programSet.getPackDirectives());
        this.sunPathRotation = programSet.getPackDirectives().getSunPathRotation();

        PackShadowDirectives shadowDirectives = programSet.getPackDirectives().getShadowDirectives();

        if (shadowDirectives.isDistanceRenderMulExplicit()) {
            if (shadowDirectives.getDistanceRenderMul() >= 0.0) {
                // add 15 and then divide by 16 to ensure we're rounding up
                forcedShadowRenderDistanceChunks = OptionalInt.of(((int) (shadowDirectives.getDistance() * shadowDirectives.getDistanceRenderMul()) + 15) / 16);
            } else {
                forcedShadowRenderDistanceChunks = OptionalInt.of(-1);
            }
        } else {
            forcedShadowRenderDistanceChunks = OptionalInt.empty();
        }

        this.customUniforms = programSet.getPack().customUniforms.build(holder -> CommonUniforms.addNonDynamicUniforms(
                holder,
                programSet.getPack().getIdMap(),
                programSet.getPackDirectives(),
                this.updateNotifier));

        // Don't clobber anything in texture unit 0. It probably won't cause issues, but we're just being cautious here.
        GL_STATE_MANAGER.glActiveTexture(GL20C.GL_TEXTURE2);

        this.customTextureManager = new CustomTextureManager(
                programSet.getPackDirectives(),
                programSet.getPack().getCustomTextureDataMap(),
                programSet.getPack().getIrisCustomTextureDataMap(),
                programSet.getPack().getCustomNoiseTexture());
        this.whitePixel = new NativeImageBackedSingleColorTexture(255, 255, 255, 255);

        GL_STATE_MANAGER.glActiveTexture(GL20C.GL_TEXTURE0);

        BufferFlipper flipper = new BufferFlipper();

        this.centerDepthSampler = new CenterDepthSampler(renderTargets::getDepthTexture, programSet.getPackDirectives().getCenterDepthHalfLife());

        this.shadowMapResolution = programSet.getPackDirectives().getShadowDirectives().getResolution();

        this.shadowTargetsSupplier = () -> {
            if (shadowRenderTargets == null) {
                // TODO: Support more than two shadowcolor render targets
                this.shadowRenderTargets = new ShadowRenderTargets(this, shadowMapResolution, shadowDirectives);
            }

            return shadowRenderTargets;
        };

        this.shadowComputes = createShadowComputes(programSet.getShadowCompute(), programSet);

        this.beginRenderer = new CompositeRenderer(
                this,
                programSet.getPackDirectives(),
                programSet.getBegin(),
                programSet.getBeginCompute(),
                renderTargets,
                shaderStorageBufferHolder,
                customTextureManager.getNoiseTexture(),
                updateNotifier,
                centerDepthSampler,
                flipper,
                shadowTargetsSupplier,
                TextureStage.BEGIN,
                customTextureManager.getCustomTextureIdMap().getOrDefault(TextureStage.BEGIN, Object2ObjectMaps.emptyMap()),
                customTextureManager.getIrisCustomTextures(),
                customImages,
                programSet.getPackDirectives().getExplicitFlips("begin_pre"),
                customUniforms);

        flippedBeforeShadow = flipper.snapshot();

        this.prepareRenderer = new CompositeRenderer(
                this,
                programSet.getPackDirectives(),
                programSet.getPrepare(),
                programSet.getPrepareCompute(),
                renderTargets,
                shaderStorageBufferHolder,
                customTextureManager.getNoiseTexture(),
                updateNotifier,
                centerDepthSampler,
                flipper,
                shadowTargetsSupplier,
                TextureStage.PREPARE,
                customTextureManager.getCustomTextureIdMap().getOrDefault(TextureStage.PREPARE, Object2ObjectMaps.emptyMap()),
                customTextureManager.getIrisCustomTextures(),
                customImages,
                programSet.getPackDirectives().getExplicitFlips("prepare_pre"),
                customUniforms);

        flippedAfterPrepare = flipper.snapshot();

        this.deferredRenderer = new CompositeRenderer(
                this,
                programSet.getPackDirectives(),
                programSet.getDeferred(),
                programSet.getDeferredCompute(),
                renderTargets,
                shaderStorageBufferHolder,
                customTextureManager.getNoiseTexture(),
                updateNotifier,
                centerDepthSampler,
                flipper,
                shadowTargetsSupplier,
                TextureStage.DEFERRED,
                customTextureManager.getCustomTextureIdMap().getOrDefault(TextureStage.DEFERRED, Object2ObjectMaps.emptyMap()),
                customTextureManager.getIrisCustomTextures(),
                customImages,
                programSet.getPackDirectives().getExplicitFlips("deferred_pre"),
                customUniforms);

        flippedAfterTranslucent = flipper.snapshot();

        this.compositeRenderer = new CompositeRenderer(
                this,
                programSet.getPackDirectives(),
                programSet.getComposite(),
                programSet.getCompositeCompute(),
                renderTargets,
                shaderStorageBufferHolder,
                customTextureManager.getNoiseTexture(),
                updateNotifier,
                centerDepthSampler,
                flipper,
                shadowTargetsSupplier,
                TextureStage.COMPOSITE_AND_FINAL,
                customTextureManager.getCustomTextureIdMap().getOrDefault(TextureStage.COMPOSITE_AND_FINAL, Object2ObjectMaps.emptyMap()),
                customTextureManager.getIrisCustomTextures(),
                customImages,
                programSet.getPackDirectives().getExplicitFlips("composite_pre"),
                customUniforms);

        this.finalPassRenderer = new FinalPassRenderer(
                this,
                programSet,
                renderTargets,
                customTextureManager.getNoiseTexture(),
                shaderStorageBufferHolder,
                updateNotifier,
                flipper.snapshot(),
                centerDepthSampler,
                shadowTargetsSupplier,
                customTextureManager.getCustomTextureIdMap().getOrDefault(TextureStage.COMPOSITE_AND_FINAL, Object2ObjectMaps.emptyMap()),
                customTextureManager.getIrisCustomTextures(),
                customImages,
                this.compositeRenderer.getFlippedAtLeastOnceFinal(),
                customUniforms);

        Supplier<ImmutableSet<Integer>> flipped = () -> isBeforeTranslucent ? flippedAfterPrepare : flippedAfterTranslucent;

        IntFunction<ProgramSamplers> createTerrainSamplers = (programId) -> {
            ProgramSamplers.Builder builder = ProgramSamplers.builder(programId, IrisSamplers.WORLD_RESERVED_TEXTURE_UNITS);

            ProgramSamplers.CustomTextureSamplerInterceptor customTextureSamplerInterceptor = ProgramSamplers.customTextureSamplerInterceptor(
                    builder,
                    customTextureManager.getCustomTextureIdMap().getOrDefault(TextureStage.GBUFFERS_AND_SHADOW, Object2ObjectMaps.emptyMap()));

            IrisSamplers.addRenderTargetSamplers(customTextureSamplerInterceptor, flipped, renderTargets, false, this);
            IrisSamplers.addCustomTextures(builder, customTextureManager.getIrisCustomTextures());

            if (!shouldBindPBR) {
                shouldBindPBR = IrisSamplers.hasPBRSamplers(customTextureSamplerInterceptor);
            }

            IrisSamplers.addLevelSamplers(customTextureSamplerInterceptor, this, (MCAbstractTexture) whitePixel, true, true, false);
            IrisSamplers.addWorldDepthSamplers(customTextureSamplerInterceptor, renderTargets);
            IrisSamplers.addNoiseSampler(customTextureSamplerInterceptor, customTextureManager.getNoiseTexture());
            IrisSamplers.addCustomImages(customTextureSamplerInterceptor, customImages);

            if (IrisSamplers.hasShadowSamplers(customTextureSamplerInterceptor)) {
                // we compiled the non-Sodium version of this program first... so if this is somehow null, something
                // very odd is going on.
                IrisSamplers.addShadowSamplers(customTextureSamplerInterceptor, Objects.requireNonNull(shadowRenderTargets), null, separateHardwareSamplers);
            }

            return builder.build();
        };

        IntFunction<ProgramImages> createTerrainImages = (programId) -> {
            ProgramImages.Builder builder = ProgramImages.builder(programId);

            IrisImages.addRenderTargetImages(builder, flipped, renderTargets);
            IrisImages.addCustomImages(builder, customImages);

            if (IrisImages.hasShadowImages(builder)) {
                // we compiled the non-Sodium version of this program first... so if this is somehow null, something
                // very odd is going on.
                IrisImages.addShadowColorImages(builder, Objects.requireNonNull(shadowRenderTargets), null);
            }

            return builder.build();
        };

        this.dhCompat = new DHCompat(this, shadowDirectives.isDhShadowEnabled().orElse(true));

        IntFunction<ProgramSamplers> createShadowTerrainSamplers = (programId) -> {
            ProgramSamplers.Builder builder = ProgramSamplers.builder(programId, IrisSamplers.WORLD_RESERVED_TEXTURE_UNITS);

            ProgramSamplers.CustomTextureSamplerInterceptor customTextureSamplerInterceptor = ProgramSamplers.customTextureSamplerInterceptor(
                    builder,
                    customTextureManager.getCustomTextureIdMap().getOrDefault(TextureStage.GBUFFERS_AND_SHADOW, Object2ObjectMaps.emptyMap()));

            IrisSamplers.addRenderTargetSamplers(customTextureSamplerInterceptor, () -> flippedBeforeShadow, renderTargets, false, this);
            IrisSamplers.addCustomTextures(builder, customTextureManager.getIrisCustomTextures());

            if (!shouldBindPBR) {
                shouldBindPBR = IrisSamplers.hasPBRSamplers(customTextureSamplerInterceptor);
            }

            IrisSamplers.addLevelSamplers(customTextureSamplerInterceptor, this, (MCAbstractTexture) whitePixel, true, true, false);
            IrisSamplers.addNoiseSampler(customTextureSamplerInterceptor, customTextureManager.getNoiseTexture());
            IrisSamplers.addCustomImages(customTextureSamplerInterceptor, customImages);

            // Only initialize these samplers if the shadow map renderer exists.
            // Otherwise, this program shouldn't be used at all?
            if (IrisSamplers.hasShadowSamplers(customTextureSamplerInterceptor)) {
                // We don't compile Sodium shadow programs unless there's a shadow pass... And a shadow pass
                // can only exist if the shadow render targets have been created by detecting their
                // usage in a different program. So this null-check makes sense here.
                IrisSamplers.addShadowSamplers(customTextureSamplerInterceptor, Objects.requireNonNull(shadowRenderTargets), null, separateHardwareSamplers);
            }

            return builder.build();
        };

        IntFunction<ProgramImages> createShadowTerrainImages = (programId) -> {
            ProgramImages.Builder builder = ProgramImages.builder(programId);

            IrisImages.addRenderTargetImages(builder, () -> flippedBeforeShadow, renderTargets);
            IrisImages.addCustomImages(builder, customImages);

            if (IrisImages.hasShadowImages(builder)) {
                // We don't compile Sodium shadow programs unless there's a shadow pass... And a shadow pass
                // can only exist if the shadow render targets have been created by detecting their
                // usage in a different program. So this null-check makes sense here.
                IrisImages.addShadowColorImages(builder, Objects.requireNonNull(shadowRenderTargets), null);
            }

            return builder.build();
        };

        this.loadedShaders = new HashSet<>();

        Stopwatch watch = Stopwatch.createStarted();

        try {
            this.shaderMap = new ShaderMap(
                    (key, syncExecutor) -> {
                        if (key.isShadow()) {
                            if (shadowRenderTargets != null) {
                                return createShadowShader(key.getName(), resolver.resolve(key.getProgram()), key, syncExecutor);
                            } else {
                                return CompletableFuture.completedFuture(null);
                            }
                        } else {
                            return createShader(key.getName(), resolver.resolve(key.getProgram()), key, syncExecutor);
                        }
                    }, getShaderKeyValues());
        } catch (IOException e) {
            destroyShaders();
            throw new RuntimeException(e);
        } catch (RuntimeException e) {
            destroyShaders();
            throw e;
        }

        watch.stop();
        IRIS_LOGGER.info("Loaded shaders in {}", watch);

        WorldRenderingSettings.INSTANCE.setEntityIds(programSet.getPack().getIdMap().getEntityIdMap());
        WorldRenderingSettings.INSTANCE.setItemIds(programSet.getPack().getIdMap().getItemIdMap());
        WorldRenderingSettings.INSTANCE.setAmbientOcclusionLevel(programSet.getPackDirectives().getAmbientOcclusionLevel());
        WorldRenderingSettings.INSTANCE.setDisableDirectionalShading(shouldDisableDirectionalShading());
        WorldRenderingSettings.INSTANCE.setUseSeparateAo(programSet.getPackDirectives().shouldUseSeparateAo());
        WorldRenderingSettings.INSTANCE.setVoxelizeLightBlocks(programSet.getPackDirectives().shouldVoxelizeLightBlocks());
        WorldRenderingSettings.INSTANCE.setSeparateEntityDraws(programSet.getPackDirectives().shouldUseSeparateEntityDraws());
        WorldRenderingSettings.INSTANCE.setUseExtendedVertexFormat(true);

        if (shadowRenderTargets == null && shadowDirectives.isShadowEnabled() == OptionalBoolean.TRUE) {
            shadowRenderTargets = new ShadowRenderTargets(this, shadowMapResolution, shadowDirectives);
        }

        if (shadowRenderTargets != null) {
            // TODO: Upstream doesn't seem to use this return value?
            boolean shadowUsesImages = checkShadowUsesImages();

            this.shadowClearPasses = ClearPassCreator.createShadowClearPasses(shadowRenderTargets, false, shadowDirectives);
            this.shadowClearPassesFull = ClearPassCreator.createShadowClearPasses(shadowRenderTargets, true, shadowDirectives);
            this.shadowCompositeRenderer = new ShadowCompositeRenderer(
                    this,
                    programSet.getPackDirectives(),
                    programSet.getShadowComposite(),
                    programSet.getShadowCompCompute(),
                    this.shadowRenderTargets,
                    this.shaderStorageBufferHolder,
                    customTextureManager.getNoiseTexture(),
                    updateNotifier,
                    customTextureManager.getCustomTextureIdMap(TextureStage.SHADOWCOMP),
                    customImages,
                    programSet.getPackDirectives().getExplicitFlips("shadowcomp_pre"),
                    customTextureManager.getIrisCustomTextures(),
                    customUniforms);

            if (programSet.getPackDirectives().getShadowDirectives().isShadowEnabled().orElse(true)) {
                this.shadowRenderer = createShadowRenderer(
                        this,
                        programSet.getShadow().orElse(null),
                        programSet.getPackDirectives(),
                        shadowRenderTargets,
                        shadowCompositeRenderer,
                        customUniforms,
                        programSet.getPack().hasFeature(FeatureFlags.SEPARATE_HARDWARE_SAMPLERS));
            } else {
                shadowRenderer = null;
            }

            defaultFBShadow = shadowRenderTargets.createFramebufferWritingToMain(new int[] { 0 });
        } else {
            this.shadowClearPasses = ImmutableList.of();
            this.shadowClearPassesFull = ImmutableList.of();
            this.shadowCompositeRenderer = null;
            this.shadowRenderer = null;
        }

        // TODO: Create fallback Sodium shaders if the pack doesn't provide terrain shaders
        //       Currently we use Sodium's shaders but they don't support EXP2 fog underwater.
        this.sodiumTerrainPipeline = new SodiumTerrainPipeline(
                this,
                programSet,
                createTerrainSamplers,
                shadowRenderTargets == null ? null : createShadowTerrainSamplers,
                createTerrainImages,
                createShadowTerrainImages,
                renderTargets,
                flippedAfterPrepare,
                flippedAfterTranslucent,
                shadowRenderTargets != null ? shadowRenderTargets.createShadowFramebuffer(
                        ImmutableSet.of(),
                        programSet.getShadow().filter(source -> !source.getDirectives().hasUnknownDrawBuffers())
                                .map(source -> source.getDirectives().getDrawBuffers()).orElse(new int[] { 0, 1 })) : null,
                customUniforms);

        this.setup = createSetupComputes(programSet.getSetup(), programSet, TextureStage.SETUP);

        // first optimization pass
        this.customUniforms.optimise();
        boolean hasRun = false;

        this.clearPassesFull = ClearPassCreator.createClearPasses(renderTargets, true, programSet.getPackDirectives().getRenderTargetDirectives());
        this.clearPasses = ClearPassCreator.createClearPasses(renderTargets, false, programSet.getPackDirectives().getRenderTargetDirectives());

        for (ComputeProgram program : setup) {
            if (program != null) {
                if (!hasRun) {
                    hasRun = true;
                    renderTargets.onFullClear();
                    Vector3d fogColor3 = CapturedRenderingState.INSTANCE.getFogColor();

                    // NB: The alpha value must be 1.0 here, or else you will get a bunch of bugs. Sildur's Vibrant Shaders
                    //     will give you pink reflections and other weirdness if this is zero.
                    Vector4f fogColor = new Vector4f((float) fogColor3.x, (float) fogColor3.y, (float) fogColor3.z, 1.0F);

                    clearPassesFull.forEach(clearPass -> clearPass.execute(fogColor));
                }
                program.use();
                program.dispatch(1, 1);
            }
        }

        if (hasRun) {
            ComputeProgram.unbind();
        }

        if (programSet.getPackDirectives().supportsColorCorrection()) {
            colorSpaceConverter = new ColorSpaceConverter() {

                @Override
                public void rebuildProgram(int width, int height, ColorSpace colorSpace) {

                }

                @Override
                public void process(int target) {

                }
            };
        } else {
            // TODO: Fix grid appearing on some devices with compute converter
            // if (IrisRenderSystem.supportsCompute()) {
            //	colorSpaceConverter = new ColorSpaceComputeConverter(mcWidth, mcHeight, IrisVideoSettings.colorSpace);
            //} else {
            colorSpaceConverter = new ColorSpaceFragmentConverter(mainFBWidth, mainFBWidth, IrisVideoSettings.colorSpace);
            //}
        }

        currentColorSpace = IrisVideoSettings.colorSpace;

        int defaultTex = packDirectives.getFallbackTex();
        defaultFB = flippedAfterPrepare.contains(defaultTex)
                ? renderTargets.createFramebufferWritingToAlt(new int[] { defaultTex })
                : renderTargets.createFramebufferWritingToMain(new int[] { defaultTex });
        defaultFBAlt = flippedAfterTranslucent.contains(defaultTex)
                ? renderTargets.createFramebufferWritingToAlt(new int[] { defaultTex })
                : renderTargets.createFramebufferWritingToMain(new int[] { defaultTex });

        blockIdsNeedPopulation = true;
    }

    protected abstract boolean checkShadowUsesImages();

    @Override
    public void destroy() {
        destroyed = true;

        destroyShaders();

        // Unbind all textures
        //
        // This is necessary because we don't want destroyed render target textures to remain bound to certain texture
        // units. Vanilla appears to properly rebind all textures as needed, and we do so too, so this does not cause
        // issues elsewhere.
        //
        // Without this code, there will be weird issues when reloading certain shaderpacks.
        for (int i = 0; i < 16; i++) {
            GL_STATE_MANAGER.glActiveTexture(GL20C.GL_TEXTURE0 + i);
            IrisRenderSystem.unbindAllSamplers();
            GL_STATE_MANAGER.bindTexture(0);
        }

        // Set the active texture unit to unit 0
        //
        // This seems to be what most code expects. It's a sane default in any case.
        GL_STATE_MANAGER.glActiveTexture(GL20C.GL_TEXTURE0);

        for (int i = 0; i < 12; i++) {
            // Clear all shader textures
            RENDER_SYSTEM.setShaderTexture(i, 0);
        }

        if (shadowCompositeRenderer != null) {
            shadowCompositeRenderer.destroy();
        }

        prepareRenderer.destroy();
        compositeRenderer.destroy();
        deferredRenderer.destroy();
        finalPassRenderer.destroy();
        centerDepthSampler.destroy();
        customTextureManager.destroy();
        whitePixel.close();

        destroyHorizonRenderer();

        GL_STATE_MANAGER.glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, 0);
        GL_STATE_MANAGER.glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, 0);
        GL_STATE_MANAGER.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, 0);

        MINECRAFT_SHIM.unbindMainFramebuffer();

        renderTargets.destroy();
        dhCompat.clearPipeline();

        customImages.forEach(GlImage::delete);


        if (shadowRenderer != null) {
            shadowRenderer.destroy();
        }

        if (shaderStorageBufferHolder != null) {
            shaderStorageBufferHolder.destroyBuffers();
        }

    }

    protected abstract void destroyHorizonRenderer();

    @Nullable
    protected abstract CommonShadowRenderer createShadowRenderer(CommonIrisRenderingPipeline commonIrisRenderingPipeline, ProgramSource programSource,
            PackDirectives packDirectives, ShadowRenderTargets shadowRenderTargets, ShadowCompositeRenderer shadowCompositeRenderer,
            CustomUniforms customUniforms, boolean b);

    protected abstract ShaderKey[] getShaderKeyValues();

    @Override
    public Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> getTextureMap() {
        return customTextureMap;
    }

    @Override
    public WorldRenderingPhase getPhase() {
        if (shouldRemovePhase) {
            phase = WorldRenderingPhase.NONE;
            shouldRemovePhase = false;
            GLDebug.popGroup();
        }

        if (overridePhase != null) {
            return overridePhase;
        }

        return phase;
    }

    public void removePhaseIfNeeded() {
        if (shouldRemovePhase) {
            phase = WorldRenderingPhase.NONE;
            shouldRemovePhase = false;
            GLDebug.popGroup();
        }
    }

    @Override
    public void setPhase(WorldRenderingPhase phase) {
        if (phase == WorldRenderingPhase.NONE) {
            if (shouldRemovePhase) GLDebug.popGroup();
            shouldRemovePhase = true;
            return;
        } else {
            shouldRemovePhase = false;
            if (phase == this.phase) {
                return;
            }
        }

        GLDebug.popGroup();
        if (phase != WorldRenderingPhase.NONE
                && phase != WorldRenderingPhase.TERRAIN_CUTOUT
                && phase != WorldRenderingPhase.TERRAIN_CUTOUT_MIPPED
                && phase != WorldRenderingPhase.TRIPWIRE) {
            if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
                GLDebug.pushGroup(phase.ordinal(), "Shadow " + StringUtils.capitalize(phase.name().toLowerCase(Locale.ROOT).replace("_", " ")));
            } else {
                GLDebug.pushGroup(phase.ordinal(), StringUtils.capitalize(phase.name().toLowerCase(Locale.ROOT).replace("_", " ")));
            }
        }
        this.phase = phase;
    }

    protected void destroyShaders() {
        // NB: If you forget this, shader reloads won't work!
        loadedShaders.forEach(shader -> {
            shader.clear();
            shader.close();
        });
    }

    @Override
    public void setOverridePhase(WorldRenderingPhase phase) {
        this.overridePhase = phase;
    }

    @Override
    public int getCurrentNormalTexture() {
        return currentNormalTexture;
    }

    @Override
    public int getCurrentSpecularTexture() {
        return currentSpecularTexture;
    }

    protected ComputeProgram[] createSetupComputes(ComputeSource[] compute, ProgramSet programSet, TextureStage stage) {
        ComputeProgram[] programs = new ComputeProgram[compute.length];
        for (int i = 0; i < programs.length; i++) {
            ComputeSource source = compute[i];
            if (source == null || !source.getSource().isPresent()) {
                continue;
            } else {
                ProgramBuilder builder;

                try {
                    String transformed = TransformPatcherBridge.patchCompute(source.getName(), source.getSource().orElse(null), stage, getTextureMap());

                    ShaderPrinter.printProgram(source.getName()).addSource(ShaderType.COMPUTE, transformed).print();

                    builder = ProgramBuilder.beginCompute(source.getName(), transformed, IrisSamplers.COMPOSITE_RESERVED_TEXTURE_UNITS);
                } catch (RuntimeException e) {
                    // TODO: Better error handling
                    throw new RuntimeException("Shader compilation failed for setup compute " + source.getName() + "!", e);
                }

                CommonUniforms.addDynamicUniforms(builder, FogMode.OFF);
                customUniforms.assignTo(builder);

                ImmutableSet<Integer> empty = ImmutableSet.of();
                Supplier<ImmutableSet<Integer>> flipped;

                flipped = () -> empty;

                TextureStage textureStage = TextureStage.SETUP;

                ProgramSamplers.CustomTextureSamplerInterceptor customTextureSamplerInterceptor = ProgramSamplers.customTextureSamplerInterceptor(
                        builder,
                        customTextureManager.getCustomTextureIdMap(textureStage));

                IrisSamplers.addRenderTargetSamplers(customTextureSamplerInterceptor, flipped, renderTargets, true, this);
                IrisSamplers.addCustomTextures(builder, customTextureManager.getIrisCustomTextures());
                IrisSamplers.addCompositeSamplers(builder, renderTargets);
                IrisSamplers.addCustomImages(customTextureSamplerInterceptor, customImages);
                IrisImages.addRenderTargetImages(builder, flipped, renderTargets);
                IrisImages.addCustomImages(builder, customImages);

                IrisSamplers.addNoiseSampler(customTextureSamplerInterceptor, customTextureManager.getNoiseTexture());

                if (IrisSamplers.hasShadowSamplers(customTextureSamplerInterceptor)) {
                    if (shadowRenderTargets != null) {
                        IrisSamplers.addShadowSamplers(customTextureSamplerInterceptor, shadowRenderTargets, null, separateHardwareSamplers);
                        IrisImages.addShadowColorImages(builder, shadowRenderTargets, null);
                    }
                }

                programs[i] = builder.buildCompute();

                this.customUniforms.mapholderToPass(builder, programs[i]);

                programs[i].setWorkGroupInfo(
                        source.getWorkGroupRelative(),
                        source.getWorkGroups(),
                        FilledIndirectPointer.basedOff(shaderStorageBufferHolder, source.getIndirectPointer()));
            }
        }

        return programs;
    }

    protected MCAbstractTexture getWhitePixel() {
        return whitePixel;
    }

    private ComputeProgram[] createShadowComputes(ComputeSource[] compute, ProgramSet programSet) {
        ComputeProgram[] programs = new ComputeProgram[compute.length];
        for (int i = 0; i < programs.length; i++) {
            ComputeSource source = compute[i];
            if (source == null || !source.getSource().isPresent()) {
                continue;
            } else {
                ProgramBuilder builder;

                try {
                    String transformed = TransformPatcherBridge.patchCompute(
                            source.getName(),
                            source.getSource().orElse(null),
                            TextureStage.GBUFFERS_AND_SHADOW,
                            getTextureMap());

                    ShaderPrinter.printProgram(source.getName()).addSource(ShaderType.COMPUTE, transformed).print();

                    builder = ProgramBuilder.beginCompute(source.getName(), transformed, IrisSamplers.WORLD_RESERVED_TEXTURE_UNITS);
                } catch (ShaderCompileException e) {
                    throw e;
                } catch (RuntimeException e) {
                    // TODO: Better error handling
                    throw new RuntimeException("Shader compilation failed for compute " + source.getName() + "!", e);
                }

                CommonUniforms.addDynamicUniforms(builder, FogMode.OFF);
                customUniforms.assignTo(builder);

                Supplier<ImmutableSet<Integer>> flipped;

                flipped = () -> flippedBeforeShadow;

                TextureStage textureStage = TextureStage.GBUFFERS_AND_SHADOW;

                ProgramSamplers.CustomTextureSamplerInterceptor customTextureSamplerInterceptor = ProgramSamplers.customTextureSamplerInterceptor(
                        builder,
                        customTextureManager.getCustomTextureIdMap(textureStage));

                IrisSamplers.addRenderTargetSamplers(customTextureSamplerInterceptor, flipped, renderTargets, false, this);
                IrisSamplers.addCustomTextures(builder, customTextureManager.getIrisCustomTextures());
                IrisSamplers.addCustomImages(customTextureSamplerInterceptor, customImages);
                IrisImages.addRenderTargetImages(builder, flipped, renderTargets);
                IrisImages.addCustomImages(builder, customImages);

                IrisSamplers.addLevelSamplers(customTextureSamplerInterceptor, this, (MCAbstractTexture) whitePixel, true, true, false);

                IrisSamplers.addNoiseSampler(customTextureSamplerInterceptor, customTextureManager.getNoiseTexture());

                if (IrisSamplers.hasShadowSamplers(customTextureSamplerInterceptor)) {
                    if (shadowRenderTargets != null) {
                        IrisSamplers.addShadowSamplers(customTextureSamplerInterceptor, shadowRenderTargets, null, separateHardwareSamplers);
                        IrisImages.addShadowColorImages(builder, shadowRenderTargets, null);
                    }
                }

                programs[i] = builder.buildCompute();

                this.customUniforms.mapholderToPass(builder, programs[i]);

                programs[i].setWorkGroupInfo(
                        source.getWorkGroupRelative(),
                        source.getWorkGroups(),
                        FilledIndirectPointer.basedOff(shaderStorageBufferHolder, source.getIndirectPointer()));
            }
        }

        return programs;
    }

    protected CompletableFuture<MCShaderInstance> createShader(String name, Optional<ProgramSource> source, ShaderKey key, Executor syncExecutor)
            throws IOException {
        if (!source.isPresent()) {
            return CompletableFuture.completedFuture(createFallbackShader(name, key));
        }

        return createShader(
                name,
                syncExecutor,
                source.get(),
                key.getProgram(),
                key.getAlphaTest(),
                key.getVertexFormat(),
                key.getFogMode(),
                key.isIntensity(),
                key.shouldIgnoreLightmap(),
                key.isGlint(),
                key.isText());
    }

    protected abstract CompletableFuture<MCShaderInstance> createShader(String name, Executor syncExecutor, ProgramSource source, ProgramId programId,
            AlphaTest fallbackAlpha, MCVertexFormat vertexFormat, FogMode fogMode, boolean isIntensity, boolean isFullbright, boolean isGlint, boolean isText)
            throws IOException;

    protected abstract MCShaderInstance createFallbackShader(String name, ShaderKey key) throws IOException;

    protected CompletableFuture<MCShaderInstance> createShadowShader(String name, Optional<ProgramSource> source, ShaderKey key, Executor syncExecutor) throws IOException {
        if (!source.isPresent()) {
            return CompletableFuture.completedFuture(createFallbackShadowShader(name, key));
        }

        return createShadowShader(name, syncExecutor, source.get(), key.getProgram(), key.getAlphaTest(), key.getVertexFormat(),
            key.isIntensity(), key.shouldIgnoreLightmap(), key.isText());
    }

    protected abstract MCShaderInstance createFallbackShadowShader(String name, ShaderKey key) throws IOException;

    protected abstract CompletableFuture<MCShaderInstance> createShadowShader(String name, Executor syncExecutor, ProgramSource source, ProgramId programId,
            AlphaTest fallbackAlpha, MCVertexFormat vertexFormat, boolean isIntensity, boolean isFullbright, boolean isText) throws IOException;
}
