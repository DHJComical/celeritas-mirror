package net.irisshaders.iris.shadows.frustum;

import net.minecraft.world.phys.AABB;

public class BoxCuller {
	private final double maxDistance;

	private double minAllowedX;
	private double maxAllowedX;
	private double minAllowedY;
	private double maxAllowedY;
	private double minAllowedZ;
	private double maxAllowedZ;

	public BoxCuller(double maxDistance) {
		this.maxDistance = maxDistance;
	}

	public void setPosition(double cameraX, double cameraY, double cameraZ) {
		this.minAllowedX = cameraX - maxDistance;
		this.maxAllowedX = cameraX + maxDistance;
		this.minAllowedY = cameraY - maxDistance;
		this.maxAllowedY = cameraY + maxDistance;
		this.minAllowedZ = cameraZ - maxDistance;
		this.maxAllowedZ = cameraZ + maxDistance;
	}

	public boolean isCulled(AABB aabb) {
		return isCulled((float) aabb.minX, (float) aabb.minY, (float) aabb.minZ,
			(float) aabb.maxX, (float) aabb.maxY, (float) aabb.maxZ);
	}

	public boolean isCulled(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
		if (maxX < this.minAllowedX || minX > this.maxAllowedX) {
			return true;
		}

		if (maxY < this.minAllowedY || minY > this.maxAllowedY) {
			return true;
		}

		return maxZ < this.minAllowedZ || minZ > this.maxAllowedZ;
	}

	public boolean isCulledSodium(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
		if (maxX < -this.maxDistance || minX > this.maxDistance) {
			return true;
		}

		if (maxY < -this.maxDistance || minY > this.maxDistance) {
			return true;
		}

		return maxZ < -this.maxDistance || minZ > this.maxDistance;
	}

	/**
	 * Camera-relative variant of a "fully inside" test: returns true only when the entire box lies within the
	 * allowed distance bounds on every axis, meaning no sub-box of it could ever be culled.
	 */
	public boolean isFullyInsideSodium(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
		return minX >= -this.maxDistance && maxX <= this.maxDistance
			&& minY >= -this.maxDistance && maxY <= this.maxDistance
			&& minZ >= -this.maxDistance && maxZ <= this.maxDistance;
	}

	@Override
	public String toString() {
		return "Box Culling active; max distance " + maxDistance;
	}
}
