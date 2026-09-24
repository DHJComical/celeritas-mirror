package org.taumc.celeritas.impl;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class Celeritas implements ClientModInitializer {
    public static final String MODID = "celeritas";
    public static String VERSION;

    @Override
    public void onInitializeClient() {
        VERSION = FabricLoader.getInstance().getModContainer(MODID).orElseThrow().getMetadata().getVersion().toString();
    }
}
