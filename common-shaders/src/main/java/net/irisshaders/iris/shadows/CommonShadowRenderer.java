package net.irisshaders.iris.shadows;

import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shaderpack.properties.PackDirectives;
import net.irisshaders.iris.shaderpack.properties.PackShadowDirectives;
import net.irisshaders.iris.shaderpack.properties.ShadowCullState;
import org.joml.Matrix4f;

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

    public CommonShadowRenderer(ProgramSource shadow, PackDirectives directives, ShadowRenderTargets shadowRenderTargets, boolean separateHardwareSamplers) {

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
            this.packHasVoxelization = shadow.getGeometrySource().isPresent();
            this.packCullingState = shadowDirectives.getCullingState();
        } else {
            this.packHasVoxelization = false;
            this.packCullingState = ShadowCullState.DEFAULT;
        }

        debugStringOverall = "half plane = " + halfPlaneLength + " meters @ " + resolution + "x" + resolution;


        this.sunPathRotation = directives.getSunPathRotation();
        this.targets = shadowRenderTargets;
    }


    protected abstract String getEntitiesDebugString();
    protected abstract String getBlockEntitiesDebugString();
}
