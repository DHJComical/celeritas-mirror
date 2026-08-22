package org.embeddedt.embeddium.impl.render.chunk.occlusion.bench;

import org.embeddedt.embeddium.impl.render.viewport.Viewport;
import org.embeddedt.embeddium.impl.render.viewport.frustum.SimpleFrustum;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Builds {@link Viewport}s from a perspective projection and camera rotation. The matrix carries no translation;
 * {@link Viewport} handles the camera-relative conversion.
 */
public final class Cameras {
    public static final float FOV_DEGREES = 70.0f;
    public static final float ASPECT = 16.0f / 9.0f;
    public static final float NEAR_PLANE = 0.05f;

    public static final float DEFAULT_YAW = 45.0f;
    public static final float DEFAULT_PITCH = 10.0f;

    private Cameras() {
    }

    public static float farPlane(int renderDistance) {
        return Math.max(256.0f, (float) (renderDistance * 16.0 * Math.sqrt(2.0)) + 32.0f);
    }

    public static Viewport viewport(double x, double y, double z, float yawDegrees, float pitchDegrees, int renderDistance) {
        Matrix4f matrix = new Matrix4f()
                .perspective((float) Math.toRadians(FOV_DEGREES), ASPECT, NEAR_PLANE, farPlane(renderDistance))
                .rotateX((float) Math.toRadians(pitchDegrees))
                .rotateY((float) Math.toRadians(yawDegrees + 180.0f));

        return new Viewport(new SimpleFrustum(new FrustumIntersection(matrix)), new Vector3d(x, y, z));
    }

    public static Viewport viewport(double x, double y, double z, int renderDistance) {
        return viewport(x, y, z, DEFAULT_YAW, DEFAULT_PITCH, renderDistance);
    }

    /** Evenly spaced yaw-rotated viewports around a fixed position. */
    public static Viewport[] yawRing(double x, double y, double z, int steps, float pitchDegrees, int renderDistance) {
        var viewports = new Viewport[steps];

        for (int i = 0; i < steps; i++) {
            viewports[i] = viewport(x, y, z, (360.0f / steps) * i, pitchDegrees, renderDistance);
        }

        return viewports;
    }

    /**
     * Unit vector pointing from the scene toward the light, as
     * {@code ShadowSearchFrustum} reports it.
     *
     * @param azimuthDegrees compass bearing of the light, 0 = +Z
     * @param elevationDegrees height above the horizon; 90 is straight overhead
     */
    public static Vector3f lightVector(float azimuthDegrees, float elevationDegrees) {
        double azimuth = Math.toRadians(azimuthDegrees);
        double elevation = Math.toRadians(elevationDegrees);
        double horizontal = Math.cos(elevation);

        return new Vector3f((float) (horizontal * Math.sin(azimuth)),
                (float) Math.sin(elevation),
                (float) (horizontal * Math.cos(azimuth))).normalize();
    }

    /**
     * Orthographic shadow viewport centred on the camera, looking along {@code light}.
     *
     * <p>Iris builds the real shadow frustum by extruding the camera frustum toward the light, which is a tighter
     * shape than this cube; a symmetric box is the conservative stand-in and keeps the benchmark independent of
     * Iris. The half-extent bounds the search on every axis, so it plays the role of the shadow render distance.
     *
     * @param light unit vector from the scene toward the light, as {@link #lightVector} builds it
     * @param halfExtentBlocks half-width of the orthographic box, in blocks
     */
    public static Viewport shadowViewport(double x, double y, double z, Vector3fc light, float halfExtentBlocks) {
        // Straight up is degenerate for the default up vector; a near-vertical sun is a real case, so pick another.
        Vector3f up = Math.abs(light.y()) > 0.999f ? new Vector3f(0.0f, 0.0f, 1.0f) : new Vector3f(0.0f, 1.0f, 0.0f);

        Matrix4f matrix = new Matrix4f()
                .ortho(-halfExtentBlocks, halfExtentBlocks, -halfExtentBlocks, halfExtentBlocks,
                        -halfExtentBlocks, halfExtentBlocks)
                // The view direction is the direction light travels: away from the light source.
                .lookAlong(-light.x(), -light.y(), -light.z(), up.x, up.y, up.z);

        return new Viewport(new SimpleFrustum(new FrustumIntersection(matrix)), new Vector3d(x, y, z));
    }

    /** Evenly spaced azimuths at a fixed elevation, modelling the sun tracking across the sky. */
    public static Vector3f[] lightRing(int steps, float elevationDegrees) {
        var lights = new Vector3f[steps];

        for (int i = 0; i < steps; i++) {
            lights[i] = lightVector((360.0f / steps) * i, elevationDegrees);
        }

        return lights;
    }

    /** Default surface-level camera: standing on the heightmap near the origin. */
    public static Viewport defaultSurfaceCamera(int renderDistance) {
        return viewport(8.5, surfaceCameraY(0, 0), 8.5, renderDistance);
    }

    /** Eye height on the SURFACE heightmap at the given section coords. */
    public static double surfaceCameraY(int sectionX, int sectionZ) {
        return (SyntheticWorld.surfaceSectionY(sectionX, sectionZ) << 4) + 9.62;
    }
}
