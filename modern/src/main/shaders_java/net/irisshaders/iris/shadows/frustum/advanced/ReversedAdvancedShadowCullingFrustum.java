package net.irisshaders.iris.shadows.frustum.advanced;

import net.irisshaders.iris.shadows.frustum.BoxCuller;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class ReversedAdvancedShadowCullingFrustum extends AdvancedShadowCullingFrustum {
	private final BoxCuller distanceCuller;

	public ReversedAdvancedShadowCullingFrustum(Matrix4f playerView, Matrix4f playerProjection, Vector3f shadowLightVectorFromOrigin, BoxCuller voxelCuller, BoxCuller distanceCuller,
												float nearPlane, float farPlane, float intervalSize) {
		super(playerView, playerProjection, shadowLightVectorFromOrigin, voxelCuller, nearPlane, farPlane, intervalSize);
		this.distanceCuller = distanceCuller;
	}

	// Everything inside the voxel distance must be rendered regardless of what the player sees, which differs from
	// the contract of normal occlusion culling
	@Override
	public boolean supportsOcclusionSearch() {
		return false;
	}

	@Override
	public void prepare(double cameraX, double cameraY, double cameraZ) {
		if (this.distanceCuller != null) {
			this.distanceCuller.setPosition(cameraX, cameraY, cameraZ);
		}
		super.prepare(cameraX, cameraY, cameraZ);
	}

	@Override
	public boolean isVisible(AABB aabb) {
		if (distanceCuller != null && distanceCuller.isCulled(aabb)) {
			return false;
		}

		if (boxCuller != null && !boxCuller.isCulled(aabb)) {
			return true;
		}

		return this.isVisible(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ) != 0;
	}

	@Override
	public int fastAabbTest(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
		if (distanceCuller != null && distanceCuller.isCulled(minX, minY, minZ, maxX, maxY, maxZ)) {
			return 0;
		}

		if (boxCuller != null && !boxCuller.isCulled(minX, minY, minZ, maxX, maxY, maxZ)) {
			return 2;
		}

		return isVisible(minX, minY, minZ, maxX, maxY, maxZ);
	}

	public boolean testAab(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
		if (distanceCuller != null && distanceCuller.isCulledSodium(minX, minY, minZ, maxX, maxY, maxZ)) {
			return false;
		}

		if (boxCuller != null && !boxCuller.isCulledSodium(minX, minY, minZ, maxX, maxY, maxZ)) {
			return true;
		}

		return this.checkCornerVisibility(minX, minY, minZ, maxX, maxY, maxZ) > 0;
	}

	@Override
	public int intersectAab(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
		if (distanceCuller != null && distanceCuller.isCulledSodium(minX, minY, minZ, maxX, maxY, maxZ)) {
			return OUTSIDE;
		}

		if (boxCuller != null && !boxCuller.isCulledSodium(minX, minY, minZ, maxX, maxY, maxZ)) {
			// The box overlaps the kept voxel region, so it is visible. It is only fully inside when the entire box
			// stays within both the voxel region and the outer distance bound, so no sub-box could be culled.
			boolean fullyInside = boxCuller.isFullyInsideSodium(minX, minY, minZ, maxX, maxY, maxZ)
				&& (distanceCuller == null || distanceCuller.isFullyInsideSodium(minX, minY, minZ, maxX, maxY, maxZ));
			return fullyInside ? FULLY_INSIDE : PARTIALLY_INSIDE;
		}

		// Outside the voxel region: fall back to the shadow planes, still bounded by the distance culler.
		int result = intersectCorners(minX, minY, minZ, maxX, maxY, maxZ);

		if (result == FULLY_INSIDE && distanceCuller != null
				&& !distanceCuller.isFullyInsideSodium(minX, minY, minZ, maxX, maxY, maxZ)) {
			return PARTIALLY_INSIDE;
		}

		return result;
	}
}
