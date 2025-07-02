package org.embeddedt.embeddium.compat.mc;

import static org.embeddedt.embeddium.compat.mc.MinecraftVersionShimService.MINECRAFT_SHIM;

public class CommonDynamicTexture implements MCDynamicTexture {
    MCDynamicTexture dynamicTexture;

    public CommonDynamicTexture(MCNativeImage mcNativeImage) {
        dynamicTexture = MINECRAFT_SHIM.createDynamicTexture(mcNativeImage);

    }

    public MCNativeImage getPixels() {
        return dynamicTexture.getPixels();
    }

    @Override
    public void upload() {
        dynamicTexture.upload();
    }

    public int getId() {
        return dynamicTexture.getId();
    }

    public void releaseId() {
        dynamicTexture.releaseId();
    }

    public void bind() {
        dynamicTexture.bind();
    }

    public void close() {
        dynamicTexture.close();
    }
}
