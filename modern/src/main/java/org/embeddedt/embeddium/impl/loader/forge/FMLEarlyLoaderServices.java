package org.embeddedt.embeddium.impl.loader.forge;

//? if forge {
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.LoadingModList;
import net.minecraftforge.fml.loading.moddiscovery.ModFile;
import net.minecraftforge.fml.loading.moddiscovery.ModFileInfo;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
//?} else if neoforge {
/*import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.fml.loading.moddiscovery.ModFile;
import net.neoforged.fml.loading.moddiscovery.ModFileInfo;
import net.neoforged.fml.loading.moddiscovery.ModInfo;
*///?}

//? if forgelike {

import org.embeddedt.embeddium.impl.loader.common.Distribution;
import org.embeddedt.embeddium.impl.loader.common.EarlyLoaderServices;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class FMLEarlyLoaderServices implements EarlyLoaderServices {
    private static final String JSON_KEY_SODIUM_OPTIONS = "sodium:options";

    @Override
    public Path findEarlyMixinFolder(String path) {
        ModFileInfo modFileInfo = FMLLoader.getLoadingModList().getModFileById("embeddium");

        if (modFileInfo == null) {
            // Probably a load error
            return null;
        }

        ModFile modFile = modFileInfo.getFile();

        //? if >=1.17 {
        Path mixinPackagePath = modFile.findResource(path.split("/"));
        //?} else
        /*Path mixinPackagePath = modFile.findResource(path);*/
        if(Files.exists(mixinPackagePath))
            return mixinPackagePath;
        else
            return null;
    }

    @Override
    public Distribution getDistribution() {
        return FMLLoader.getDist().isClient() ? Distribution.CLIENT : Distribution.SERVER;
    }

    @Override
    public boolean isLoadingNormally() {
        //? if neoforge && >=1.20.6
        /*return !FMLLoader.getLoadingModList().hasErrors();*/
        //? if forge || (neoforge && <1.20.6)
        return FMLLoader.getLoadingModList().getErrors().isEmpty();
    }

    public List<String> getLoadedModIds() {
        return FMLLoader.getLoadingModList().getMods().stream().map(ModInfo::getModId).toList();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FMLLoader.getLoadingModList().getModFileById(modId) != null;
    }

    @Override
    public void readModMixinConfigOverrides(Consumer<MixinConfigOverride> consumer) {
        // Example of how to put overrides into the mods.toml file:
        // ...
        // [[mods]]
        // modId="examplemod"
        // [mods."sodium:options"]
        // "features.chunk_rendering"=false
        // ...
        for (var meta : LoadingModList.get().getMods()) {
            meta.getConfigElement(JSON_KEY_SODIUM_OPTIONS).ifPresent(overridesObj -> {
                if (overridesObj instanceof Map overrides && overrides.keySet().stream().allMatch(key -> key instanceof String)) {
                    overrides.forEach((key, value) -> {
                        if(value instanceof Boolean flag)
                            consumer.accept(new MixinConfigOverride(meta.getModId(), (String)key, flag));
                    });
                }
            });
        }
    }
}
//?}
