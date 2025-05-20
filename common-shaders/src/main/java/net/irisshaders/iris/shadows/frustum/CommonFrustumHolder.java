package net.irisshaders.iris.shadows.frustum;


public class CommonFrustumHolder {
    protected CommonFrustum frustum;
    protected String distanceInfo = "(unavailable)";
    protected String cullingInfo = "(unavailable)";

    public String getDistanceInfo() {
        return distanceInfo;
    }

    public String getCullingInfo() {
        return cullingInfo;
    }

    public CommonFrustumHolder setInfo(CommonFrustum frustum, String distanceInfo, String cullingInfo) {
        this.frustum = frustum;
        this.distanceInfo = distanceInfo;
        this.cullingInfo = cullingInfo;
        return this;
    }

    public CommonFrustum getFrustum() {
        return frustum;
    }
}
