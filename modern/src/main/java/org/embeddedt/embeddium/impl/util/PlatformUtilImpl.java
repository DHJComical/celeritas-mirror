package org.embeddedt.embeddium.impl.util;

import java.nio.file.Path;

import org.embeddedt.embeddium.compat.mc.PlatformUtilService;

//? if forge {
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;

public class PlatformUtilImpl implements PlatformUtilService {
    public boolean isLoadValid() {
        return FMLLoader.getLoadingModList().getErrors().isEmpty();
    }

    public boolean modPresent(String modid) {
        return FMLLoader.getLoadingModList().getModFileById(modid) != null;
    }

    public String getModName(String modId) {
        return ModList.get().getModContainerById(modId).map(container -> container.getModInfo().getDisplayName()).orElse(modId);
    }

    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.isProduction();
    }

    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    public Path getGameDir() {
        return FMLPaths.GAMEDIR.get();
    }
}
//?} else if fabric {
/*import net.fabricmc.loader.api.FabricLoader;
import org.embeddedt.embeddium.impl.loader.common.EarlyLoaderServices;

public class PlatformUtilImpl implements PlatformUtilService {
    public boolean isLoadValid() {
        return true;
    }

    public boolean modPresent(String modid) {
        return EarlyLoaderServices.INSTANCE.isModLoaded(modid);
    }

    public String getModName(String modId) {
        return FabricLoader.getInstance().getModContainer(modId).map(c -> c.getMetadata().getName()).orElse(modId);
    }

    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    public Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    public Path getGameDir() {
        return FabricLoader.getInstance().getGameDir();
    }
}
*///?} else if neoforge {
/*import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;

public class PlatformUtilImpl implements PlatformUtilService {
    public boolean isLoadValid() {
        //? if >=1.20.6 {
        /^return !FMLLoader.getLoadingModList().hasErrors();
        ^///?} else
        return FMLLoader.getLoadingModList().getErrors().isEmpty();
    }

    public boolean modPresent(String modid) {
        return FMLLoader.getLoadingModList().getModFileById(modid) != null;
    }

    public String getModName(String modId) {
        return ModList.get().getModContainerById(modId).map(container -> container.getModInfo().getDisplayName()).orElse(modId);
    }

    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.isProduction();
    }

    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    public Path getGameDir() {
        return FMLPaths.GAMEDIR.get();
    }
}
*///?}
