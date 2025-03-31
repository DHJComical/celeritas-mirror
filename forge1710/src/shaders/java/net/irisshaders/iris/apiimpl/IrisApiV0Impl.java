package net.irisshaders.iris.apiimpl;

import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.api.v0.IrisApiConfig;
import net.irisshaders.iris.api.v0.IrisTextVertexSink;

import java.nio.ByteBuffer;
import java.util.function.IntFunction;

public class IrisApiV0Impl implements IrisApi {
    @Override
    public int getMinorApiRevision() {
        return 2;
    }

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
    public IrisTextVertexSink createTextVertexSink(int maxQuadCount, IntFunction<ByteBuffer> bufferProvider) {
        return null;
    }

    @Override
    public float getSunPathRotation() {
        return 0;
    }
}
