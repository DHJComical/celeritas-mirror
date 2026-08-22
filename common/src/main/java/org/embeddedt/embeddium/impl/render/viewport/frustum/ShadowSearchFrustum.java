package org.embeddedt.embeddium.impl.render.viewport.frustum;

/**
 * Optional capability of a shadow-pass {@link Frustum}: a directional light whose shadow casters may be found by a
 * receiver-driven occlusion search instead of a plain frustum scan.
 *
 * <p>A shadow frustum that implements this and reports {@link #supportsOcclusionSearch()} asserts that the shadow
 * map is only sampled for points visible to the player camera (direct shadowing, volumetric light and similar), so
 * any section that cannot lie on a light ray through a camera-visible section may be omitted from the shadow pass.
 * Frustums that do not implement this, or return {@code false}, receive the frustum-only scan.
 */
public interface ShadowSearchFrustum {
    boolean supportsOcclusionSearch();

    float shadowLightX();

    float shadowLightY();

    float shadowLightZ();
}
