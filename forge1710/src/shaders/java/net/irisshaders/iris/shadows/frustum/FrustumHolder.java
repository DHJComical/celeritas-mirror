package net.irisshaders.iris.shadows.frustum;

import net.minecraft.client.renderer.culling.Frustrum;

public class FrustumHolder extends CommonFrustumHolder {
    private Frustrum frustum;

    public FrustumHolder setInfo(Frustrum frustum, String distanceInfo, String cullingInfo) {
        super.setInfo(distanceInfo, cullingInfo);
        this.frustum = frustum;
        return this;
    }

    public Frustrum getFrustum() {
        return frustum;
    }

}
