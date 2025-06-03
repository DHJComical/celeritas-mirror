package net.irisshaders.iris.pipeline;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.irisshaders.iris.gl.buffer.ShaderStorageBufferHolder;
import net.irisshaders.iris.gl.framebuffer.GlFramebuffer;
import net.irisshaders.iris.gl.image.GlImage;
import net.irisshaders.iris.gl.texture.TextureType;
import net.irisshaders.iris.gui.option.IrisVideoSettings;
import net.irisshaders.iris.helpers.Tri;
import net.irisshaders.iris.pathways.colorspace.ColorSpace;
import net.irisshaders.iris.shaderpack.ImageInformation;
import net.irisshaders.iris.shaderpack.ShaderPack;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.irisshaders.iris.shaderpack.properties.CloudSetting;
import net.irisshaders.iris.shaderpack.properties.PackDirectives;
import net.irisshaders.iris.shaderpack.properties.PackShadowDirectives;
import net.irisshaders.iris.shaderpack.texture.TextureStage;
import net.irisshaders.iris.shadows.ShadowRenderTargets;
import net.irisshaders.iris.shadows.ShadowRenderingState;
import net.irisshaders.iris.targets.ClearPass;
import net.irisshaders.iris.uniforms.CommonUniforms;
import net.irisshaders.iris.uniforms.FrameUpdateNotifier;
import net.irisshaders.iris.uniforms.custom.CustomUniforms;
import org.apache.commons.lang3.StringUtils;
import org.embeddedt.embeddium.impl.gl.debug.GLDebug;

import java.util.HashSet;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Supplier;

public abstract class CommonIrisRenderingPipeline implements WorldRenderingPipeline {
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

    public CommonIrisRenderingPipeline(ProgramSet programSet, int mainFBWidth, int mainFBHeight) {
        PackShadowDirectives shadowDirectives = programSet.getPackDirectives().getShadowDirectives();

        if (shadowDirectives.isDistanceRenderMulExplicit()) {
            if (shadowDirectives.getDistanceRenderMul() >= 0.0) {
                // add 15 and then divide by 16 to ensure we're rounding up
                forcedShadowRenderDistanceChunks =
                        OptionalInt.of(((int) (shadowDirectives.getDistance() * shadowDirectives.getDistanceRenderMul()) + 15) / 16);
            } else {
                forcedShadowRenderDistanceChunks = OptionalInt.of(-1);
            }
        } else {
            forcedShadowRenderDistanceChunks = OptionalInt.empty();
        }


        this.shadowMapResolution = programSet.getPackDirectives().getShadowDirectives().getResolution();
        this.packDirectives = programSet.getPackDirectives();
        this.shadowDirectives = packDirectives.getShadowDirectives();

        this.shadowTargetsSupplier = () -> {
            if (shadowRenderTargets == null) {
                // TODO: Support more than two shadowcolor render targets
                this.shadowRenderTargets = new ShadowRenderTargets(this, shadowMapResolution, shadowDirectives);
            }

            return shadowRenderTargets;
        };

        this.customImages = new HashSet<>();
        for (ImageInformation information : programSet.getPack().getIrisCustomImages()) {
            if (information.isRelative()) {
                customImages.add(new GlImage.Relative(information.name(), information.samplerName(), information.format(), information.internalTextureFormat(), information.type(), information.clear(), information.relativeWidth(), information.relativeHeight(), mainFBWidth, mainFBHeight));
            } else {
                customImages.add(new GlImage(information.name(), information.samplerName(), information.target(), information.format(), information.internalTextureFormat(), information.type(), information.clear(), information.width(), information.height(), information.depth()));
            }
        }
        this.clearImages = customImages.stream().filter(GlImage::shouldClear).toArray(GlImage[]::new);
        this.pack = programSet.getPack();
        this.updateNotifier = new FrameUpdateNotifier();

        this.customUniforms = programSet.getPack().customUniforms.build(
                holder -> CommonUniforms.addNonDynamicUniforms(holder, programSet.getPack().getIdMap(), programSet.getPackDirectives(), this.updateNotifier)
        );

        this.sunPathRotation = programSet.getPackDirectives().getSunPathRotation();
        this.shouldRenderUnderwaterOverlay = programSet.getPackDirectives().underwaterOverlay();
        this.shouldRenderVignette = programSet.getPackDirectives().vignette();
        this.shouldWriteRainAndSnowToDepthBuffer = programSet.getPackDirectives().rainDepth();
        this.oldLighting = programSet.getPackDirectives().isOldLighting();
        this.frustumCulling = programSet.getPackDirectives().shouldUseFrustumCulling();
        this.occlusionCulling = programSet.getPackDirectives().shouldUseOcclusionCulling();
        this.shouldRenderSun = programSet.getPackDirectives().shouldRenderSun();
        this.shouldRenderMoon = programSet.getPackDirectives().shouldRenderMoon();
        this.allowConcurrentCompute = programSet.getPackDirectives().getConcurrentCompute();
        this.cloudSetting = programSet.getPackDirectives().getCloudSetting();
        currentColorSpace = IrisVideoSettings.colorSpace;
        this.customTextureMap = programSet.getPackDirectives().getTextureMap();
    }

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
        if (phase != WorldRenderingPhase.NONE && phase != WorldRenderingPhase.TERRAIN_CUTOUT && phase != WorldRenderingPhase.TERRAIN_CUTOUT_MIPPED && phase != WorldRenderingPhase.TRIPWIRE) {
            if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
                GLDebug.pushGroup(phase.ordinal(), "Shadow " + StringUtils.capitalize(phase.name().toLowerCase(Locale.ROOT).replace("_", " ")));
            } else {
                GLDebug.pushGroup(phase.ordinal(), StringUtils.capitalize(phase.name().toLowerCase(Locale.ROOT).replace("_", " ")));
            }
        }
		this.phase = phase;
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
}
