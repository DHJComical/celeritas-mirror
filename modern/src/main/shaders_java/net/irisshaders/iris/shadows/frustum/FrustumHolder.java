package net.irisshaders.iris.shadows.frustum;

import net.minecraft.client.renderer.culling.Frustum;

public class FrustumHolder extends CommonFrustumHolder {
	private Frustum frustum;

    public FrustumHolder setInfo(Frustum frustum, String distanceInfo, String cullingInfo) {
        super.setInfo(distanceInfo, cullingInfo);
		this.frustum = frustum;
		return this;
	}

	public Frustum getFrustum() {
		return frustum;
	}

}
