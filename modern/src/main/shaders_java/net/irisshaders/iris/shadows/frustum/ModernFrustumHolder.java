package net.irisshaders.iris.shadows.frustum;

import net.irisshaders.iris.compat.mc.FrustumWrapper;

public class ModernFrustumHolder extends CommonFrustumHolder {
    private FrustumWrapper wrapper;

    @Override
    public CommonFrustumHolder setInfo(CommonFrustum frustum, String distanceInfo, String cullingInfo) {
        super.setInfo(frustum, distanceInfo, cullingInfo);
        this.wrapper = new FrustumWrapper(frustum);
        return this;
    }

    public FrustumWrapper getWrapper() {
        return wrapper;
    }
}
