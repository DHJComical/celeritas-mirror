package org.embeddedt.embeddium.impl.util;

import net.fabricmc.loader.api.FabricLoader;
import org.embeddedt.embeddium.impl.loader.common.EarlyLoaderServices;

import java.nio.file.Path;

//? if forge {
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;

public class PlatformUtil {
    public static boolean isLoadValid() {
        return FMLLoader.getLoadingModList().getErrors().isEmpty();
    }

    public static boolean modPresent(String modid) {
        return FMLLoader.getLoadingModList().getModFileById(modid) != null;
    }

    public static String getModName(String modId) {
        return ModList.get().getModContainerById(modId).map(container -> container.getModInfo().getDisplayName()).orElse(modId);
    }

    public static boolean isDevelopmentEnvironment() {
        return !FMLLoader.isProduction();
    }

    public static Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    public static Path getGameDir() {
        return FMLPaths.GAMEDIR.get();
    }
}
//?} else if fabric {
/*public class PlatformUtil {
    public static boolean isLoadValid() {
        return true;
    }

    public static boolean modPresent(String modid) {
        return EarlyLoaderServices.INSTANCE.isModLoaded(modid);
    }

    public static String getModName(String modId) {
        return FabricLoader.getInstance().getModContainer(modId).map(c -> c.getMetadata().getName()).orElse(modId);
    }

    public static boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    public static Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    public static Path getGameDir() {
        return FabricLoader.getInstance().getGameDir();
    }
}
*///?} else if neoforge {
/*import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;

public class PlatformUtil {
    public static boolean isLoadValid() {
        //? if >=1.21.11 {
        /^return !FMLLoader.getCurrent().getLoadingModList().hasErrors();
        ^///?} else if >=1.20.6 {
        /^return !FMLLoader.getLoadingModList().hasErrors();
        ^///?} else
        return FMLLoader.getLoadingModList().getErrors().isEmpty();
    }

    public static boolean modPresent(String modid) {
        //? if >=1.21.11 {
        /^return FMLLoader.getCurrent().getLoadingModList().getModFileById(modid) != null;
        ^///?} else {
        return FMLLoader.getLoadingModList().getModFileById(modid) != null;
        //?}
    }

    public static String getModName(String modId) {
        return ModList.get().getModContainerById(modId).map(container -> container.getModInfo().getDisplayName()).orElse(modId);
    }

    public static boolean isDevelopmentEnvironment() {
        //? if >=1.21.11 {
        /^return !FMLLoader.getCurrent().isProduction();
        ^///?} else
        return !FMLLoader.isProduction();
    }

    public static Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    public static Path getGameDir() {
        return FMLPaths.GAMEDIR.get();
    }
}
*///?}
