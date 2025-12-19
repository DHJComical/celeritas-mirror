package org.taumc.celeritas.impl;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.embeddedt.embeddium.impl.gl.device.GLRenderDevice;
import org.lwjgl.opengl.GL15C;

public class Celeritas implements ClientModInitializer {
    public static final String MODID = "celeritas";
    public static String VERSION;

    @Override
    public void onInitializeClient() {
        GLRenderDevice.VANILLA_STATE_RESETTER = () -> {
            GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0);
        };
        VERSION = FabricLoader.getInstance().getModContainer(MODID).orElseThrow().getMetadata().getVersion().toString();
    }
}
