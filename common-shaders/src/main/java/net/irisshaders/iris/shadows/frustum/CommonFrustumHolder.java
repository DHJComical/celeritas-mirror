package net.irisshaders.iris.shadows.frustum;

public abstract class CommonFrustumHolder {
    protected String distanceInfo = "(unavailable)";
    protected String cullingInfo = "(unavailable)";

    public String getDistanceInfo() {
        return distanceInfo;
    }

    public String getCullingInfo() {
        return cullingInfo;
    }

    protected void setInfo(String distanceInfo, String cullingInfo) {
        this.distanceInfo = distanceInfo;
        this.cullingInfo = cullingInfo;
    }
}
