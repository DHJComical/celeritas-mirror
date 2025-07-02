package org.embeddedt.embeddium.compat.mc;

public interface MCDynamicTexture extends MCAbstractTexture {
    MCNativeImage getPixels();
    void upload();
}
