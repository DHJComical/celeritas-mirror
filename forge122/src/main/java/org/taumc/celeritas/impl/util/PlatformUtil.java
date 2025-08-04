package org.taumc.celeritas.impl.util;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.Loader;

import java.io.File;

public class PlatformUtil {

    public static boolean isLoadValid() {
        //TODO Implement. Doesn't seem to be used outside of shaders
        return true;
    }

    public static boolean modPresent(String modid) {
        return Loader.isModLoaded(modid);
    }

    public static String getModName(String modId) {
        return Loader.instance().getIndexedModList().get(modId).getName();
    }

    public static File getConfigDir() {
        return Loader.instance().getConfigDir();
    }

    public static File getGameDir() {
        return Minecraft.getMinecraft().gameDir;
    }
}
