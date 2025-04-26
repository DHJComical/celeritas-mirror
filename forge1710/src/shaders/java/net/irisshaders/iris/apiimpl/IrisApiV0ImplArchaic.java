package net.irisshaders.iris.apiimpl;

import net.irisshaders.iris.api.v0.IrisApiConfig;
import org.taumc.celeritas.api.v0.CeleritasShadersApi;

public class IrisApiV0ImplArchaic implements CeleritasShadersApi {
    @Override
    public boolean isShaderPackInUse() {
        return false;
    }

    @Override
    public boolean isRenderingShadowPass() {
        return false;
    }

    @Override
    public int getOverriddenShadowDistance(int base) {
        return 0;
    }

    @Override
    public boolean isShadowDistanceSliderEnabled() {
        return false;
    }

    @Override
    public boolean areDebugOptionsEnabled() {
        return false;
    }

    @Override
    public Object openMainIrisScreenObj(Object parent) {
        return null;
    }

    @Override
    public String getMainScreenLanguageKey() {
        return "";
    }

    @Override
    public IrisApiConfig getConfig() {
        return null;
    }

    @Override
    public float getSunPathRotation() {
        return 0;
    }


}
