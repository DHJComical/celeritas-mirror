package org.embeddedt.embeddium.impl.render.chunk.occlusion.bench;

import org.embeddedt.embeddium.impl.render.viewport.Viewport;
import org.embeddedt.embeddium.impl.render.viewport.frustum.SimpleFrustum;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3d;

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

    /** Default surface-level camera: standing on the heightmap near the origin. */
    public static Viewport defaultSurfaceCamera(int renderDistance) {
        return viewport(8.5, surfaceCameraY(0, 0), 8.5, renderDistance);
    }

    /** Eye height on the SURFACE heightmap at the given section coords. */
    public static double surfaceCameraY(int sectionX, int sectionZ) {
        return (SyntheticWorld.surfaceSectionY(sectionX, sectionZ) << 4) + 9.62;
    }
}
