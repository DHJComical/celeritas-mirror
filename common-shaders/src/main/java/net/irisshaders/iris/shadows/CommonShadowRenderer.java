package net.irisshaders.iris.shadows;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.irisshaders.iris.IrisCommon;
import net.irisshaders.iris.IrisConstants;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.gui.option.IrisVideoSettings;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shaderpack.properties.PackDirectives;
import net.irisshaders.iris.shaderpack.properties.PackShadowDirectives;
import net.irisshaders.iris.shaderpack.properties.ShadowCullState;
import net.irisshaders.iris.shadows.frustum.CommonFrustumHolder;
import net.irisshaders.iris.uniforms.CelestialUniforms;
import org.embeddedt.embeddium.compat.mc.MCCamera;
import org.embeddedt.embeddium.compat.mc.MCLevelRenderer;
import org.embeddedt.embeddium.impl.gl.shader.ShaderType;
import org.joml.Matrix4f;
import org.lwjgl.opengl.ARBTextureSwizzle;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;

import java.util.ArrayList;
import java.util.List;

import static com.mitchej123.glsm.RenderSystemService.RENDER_SYSTEM;
import static org.embeddedt.embeddium.compat.mc.MinecraftVersionShimService.MINECRAFT_SHIM;

public abstract class CommonShadowRenderer {
    public static boolean ACTIVE = false;
    public static int renderDistance;
    public static Matrix4f MODELVIEW;
    public static Matrix4f PROJECTION;
    protected final String debugStringOverall;
    protected final float halfPlaneLength;
    protected final int resolution;
    protected final float nearPlane;
    protected final float farPlane;
    protected final float voxelDistance;
    protected final float renderDistanceMultiplier;
    protected final float entityShadowDistanceMultiplier;
    protected final float intervalSize;
    protected final Float fov;
    protected final boolean shouldRenderTerrain;
    protected final boolean shouldRenderTranslucent;
    protected final boolean shouldRenderEntities;
    protected final boolean shouldRenderPlayer;
    protected final boolean shouldRenderBlockEntities;
    protected final boolean shouldRenderDH;
    protected final float sunPathRotation;
    protected final boolean separateHardwareSamplers;
    protected final boolean shouldRenderLightBlockEntities;
    protected final ShadowCullState packCullingState;
    protected final ShadowRenderTargets targets;
    protected boolean packHasVoxelization;
    protected String debugStringTerrain = "(unavailable)";
    protected int renderedShadowEntities = 0;
    protected int renderedShadowBlockEntities = 0;
    private final List<MipmapPass> mipmapPasses = new ArrayList<>();

    protected CommonFrustumHolder terrainFrustumHolder;
    protected CommonFrustumHolder entityFrustumHolder;

    public CommonShadowRenderer(ProgramSource shadow, PackDirectives directives, ShadowRenderTargets shadowRenderTargets, boolean separateHardwareSamplers) {
        initFrustumHolders();
        this.separateHardwareSamplers = separateHardwareSamplers;
        final PackShadowDirectives shadowDirectives = directives.getShadowDirectives();
        this.halfPlaneLength = shadowDirectives.getDistance();
        this.nearPlane = shadowDirectives.getNearPlane();
        this.farPlane = shadowDirectives.getFarPlane();

        this.voxelDistance = shadowDirectives.getVoxelDistance();
        this.renderDistanceMultiplier = shadowDirectives.getDistanceRenderMul();
        this.entityShadowDistanceMultiplier = shadowDirectives.getEntityShadowDistanceMul();

        this.resolution = shadowDirectives.getResolution();
        this.intervalSize = shadowDirectives.getIntervalSize();
        this.shouldRenderTerrain = shadowDirectives.shouldRenderTerrain();
        this.shouldRenderTranslucent = shadowDirectives.shouldRenderTranslucent();
        this.shouldRenderEntities = shadowDirectives.shouldRenderEntities();
        this.shouldRenderPlayer = shadowDirectives.shouldRenderPlayer();
        this.shouldRenderBlockEntities = shadowDirectives.shouldRenderBlockEntities();
        this.shouldRenderLightBlockEntities = shadowDirectives.shouldRenderLightBlockEntities();
        this.shouldRenderDH = shadowDirectives.isDhShadowEnabled().orElse(false);

        this.fov = shadowDirectives.getFov();

        if (shadow != null) {
            // Assume that the shader pack is doing voxelization if a geometry shader is detected.
            // Also assume voxelization if image load / store is detected.
            this.packHasVoxelization = shadow.getSource(ShaderType.GEOM).isPresent();
            this.packCullingState = shadowDirectives.getCullingState();
        } else {
            this.packHasVoxelization = false;
            this.packCullingState = ShadowCullState.DEFAULT;
        }

        debugStringOverall = "half plane = " + halfPlaneLength + " meters @ " + resolution + "x" + resolution;


        this.sunPathRotation = directives.getSunPathRotation();
        this.targets = shadowRenderTargets;
    }

    protected abstract void initFrustumHolders();

    protected static float getSkyAngle() {
        return MINECRAFT_SHIM.getSkyAngle();
    }

    protected static float getSunAngle() {
		float skyAngle = getSkyAngle();

		if (skyAngle < 0.75F) {
			return skyAngle + 0.25F;
		} else {
			return skyAngle - 0.75F;
		}
	}

    protected static float getShadowAngle() {
		float shadowAngle = getSunAngle();

		if (!CelestialUniforms.isDay()) {
			shadowAngle -= 0.5F;
		}

		return shadowAngle;
	}

    protected void configureSamplingSettings(PackShadowDirectives shadowDirectives) {
        final ImmutableList<PackShadowDirectives.DepthSamplingSettings> depthSamplingSettings =
            shadowDirectives.getDepthSamplingSettings();

        final Int2ObjectMap<PackShadowDirectives.SamplingSettings> colorSamplingSettings =
            shadowDirectives.getColorSamplingSettings();

        RENDER_SYSTEM.glActiveTexture(GL20C.GL_TEXTURE4);

        configureDepthSampler(targets.getDepthTexture().getTextureId(), depthSamplingSettings.get(0));

        configureDepthSampler(targets.getDepthTextureNoTranslucents().getTextureId(), depthSamplingSettings.get(1));

        for (int i = 0; i < targets.getNumColorTextures(); i++) {
            if (targets.get(i) != null) {
                int glTextureId = targets.get(i).getMainTexture();

                configureSampler(glTextureId, colorSamplingSettings.computeIfAbsent(i, a -> new PackShadowDirectives.SamplingSettings()));
            }
        }

        RENDER_SYSTEM.glActiveTexture(GL20C.GL_TEXTURE0);
    }

    private void configureDepthSampler(int glTextureId, PackShadowDirectives.DepthSamplingSettings settings) {
        if (settings.getHardwareFiltering() && !separateHardwareSamplers) {
            // We have to do this or else shadow hardware filtering breaks entirely!
            IrisRenderSystem.texParameteri(glTextureId, GL20C.GL_TEXTURE_2D, GL20C.GL_TEXTURE_COMPARE_MODE, GL30C.GL_COMPARE_REF_TO_TEXTURE);
        }

        // Workaround for issues with old shader packs like Chocapic v4.
        // They expected the driver to put the depth value in z, but it's supposed to only
        // be available in r. So we set up the swizzle to fix that.
        IrisRenderSystem.texParameteriv(glTextureId, GL20C.GL_TEXTURE_2D, ARBTextureSwizzle.GL_TEXTURE_SWIZZLE_RGBA,
            new int[]{GL30C.GL_RED, GL30C.GL_RED, GL30C.GL_RED, GL30C.GL_ONE});

        configureSampler(glTextureId, settings);
    }

    private void configureSampler(int glTextureId, PackShadowDirectives.SamplingSettings settings) {
        if (settings.getMipmap()) {
            int filteringMode = settings.getNearest() ? GL20C.GL_NEAREST_MIPMAP_NEAREST : GL20C.GL_LINEAR_MIPMAP_LINEAR;
            mipmapPasses.add(new MipmapPass(glTextureId, filteringMode));
        }

        if (!settings.getNearest()) {
            // Make sure that things are smoothed
            IrisRenderSystem.texParameteri(glTextureId, GL20C.GL_TEXTURE_2D, GL20C.GL_TEXTURE_MIN_FILTER, GL20C.GL_LINEAR);
            IrisRenderSystem.texParameteri(glTextureId, GL20C.GL_TEXTURE_2D, GL20C.GL_TEXTURE_MAG_FILTER, GL20C.GL_LINEAR);
        } else {
            IrisRenderSystem.texParameteri(glTextureId, GL20C.GL_TEXTURE_2D, GL20C.GL_TEXTURE_MIN_FILTER, GL20C.GL_NEAREST);
            IrisRenderSystem.texParameteri(glTextureId, GL20C.GL_TEXTURE_2D, GL20C.GL_TEXTURE_MAG_FILTER, GL20C.GL_NEAREST);
        }
    }

    protected void generateMipmaps() {
        RENDER_SYSTEM.glActiveTexture(GL20C.GL_TEXTURE4);

        for (MipmapPass mipmapPass : mipmapPasses) {
            setupMipmappingForTexture(mipmapPass.texture(), mipmapPass.targetFilteringMode());
        }

        RENDER_SYSTEM.glActiveTexture(GL20C.GL_TEXTURE0);
    }

    protected void setupMipmappingForTexture(int texture, int filteringMode) {
        IrisRenderSystem.generateMipmaps(texture, GL20C.GL_TEXTURE_2D);
        IrisRenderSystem.texParameteri(texture, GL20C.GL_TEXTURE_2D, GL20C.GL_TEXTURE_MIN_FILTER, filteringMode);
    }

    public void addDebugText(List<String> messages) {
        if (IrisVideoSettings.getOverriddenShadowDistance(IrisVideoSettings.shadowDistance) == 0) {
            messages.add("[" + IrisConstants.MODNAME + "] Shadow Maps: off, shadow distance 0");
            return;
        }

        if (IrisCommon.getIrisConfig().areDebugOptionsEnabled()) {
            messages.add("[" + IrisConstants.MODNAME + "] Shadow Maps: " + debugStringOverall);
            messages.add("[" + IrisConstants.MODNAME + "] Shadow Distance Terrain: " + terrainFrustumHolder.getDistanceInfo() + " Entity: " + entityFrustumHolder.getDistanceInfo());
            messages.add("[" + IrisConstants.MODNAME + "] Shadow Culling Terrain: " + terrainFrustumHolder.getCullingInfo() + " Entity: " + entityFrustumHolder.getCullingInfo());
            messages.add("[" + IrisConstants.MODNAME + "] Shadow Terrain: " + debugStringTerrain
                    + (shouldRenderTerrain ? "" : " (no terrain) ") + (shouldRenderTranslucent ? "" : "(no translucent)"));
            messages.add("[" + IrisConstants.MODNAME + "] Shadow Entities: " + getEntitiesDebugString());
            messages.add("[" + IrisConstants.MODNAME + "] Shadow Block Entities: " + getBlockEntitiesDebugString());

            addBuffersDebugText(messages);
        } else {
            messages.add("[" + IrisConstants.MODNAME + "] Shadow info: " + debugStringTerrain);
            messages.add("[" + IrisConstants.MODNAME + "] E: " + renderedShadowEntities);
            messages.add("[" + IrisConstants.MODNAME + "] BE: " + renderedShadowBlockEntities);
        }
    }

    public abstract void destroy();

    protected abstract String getEntitiesDebugString();
    protected abstract String getBlockEntitiesDebugString();
    protected abstract void addBuffersDebugText(List<String> messages);
    public abstract void renderShadows(MCLevelRenderer levelRendererIn, MCCamera playerCamera);

    protected record MipmapPass(int texture, int targetFilteringMode) {
    }

}
