package org.taumc.celeritas.interfaces;

public interface IRenderTargetExt {
    int iris$getDepthBufferVersion();

    int iris$getColorBufferVersion();

    boolean getIris$useDepth();
    int getIris$depthTextureId();
}
