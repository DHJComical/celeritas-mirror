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
import org.embeddedt.embeddium.impl.util.MixinClassValidator;
import org.embeddedt.embeddium.impl.util.PlatformUtil;

//? if <1.21.10 {
import java.nio.file.Files;
import java.nio.file.Path;
//?}
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class FMLEarlyLoaderServices implements EarlyLoaderServices {
    private static final String JSON_KEY_SODIUM_OPTIONS = "sodium:options";

    @Override
    public List<String> findEarlyMixinClasses(String packagePath) {
        ModFileInfo modFileInfo = FMLLoader/*? if >=1.21.10 {*//*.getCurrent()*//*?}*/.getLoadingModList().getModFileById("embeddium");

        if (modFileInfo == null) {
            // Probably a load error
            return List.of();
        }

        ModFile modFile = modFileInfo.getFile();

        //? if >=1.21.10 {
        /*String startingFolder = packagePath.endsWith("/") ? packagePath.substring(0, packagePath.length() - 1) : packagePath;
        List<String> mixins = new java.util.ArrayList<>();
        modFile.getContents().visitContent(startingFolder, (relativePath, resource) -> {
            relativePath = relativePath.substring(startingFolder.length() + 1);
            if (relativePath.indexOf('$') != -1 || !relativePath.endsWith(".class")) {
                return;
            }
            try {
                if (MixinClassValidator.isMixinClass(MixinClassValidator.fromBytecode(resource.readAllBytes()))) {
                    mixins.add(relativePath.substring(0, relativePath.length() - 6).replace('/', '.'));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        return mixins;
        *///?} else {
        //? if >=1.17 {
        Path mixinPackagePath = modFile.findResource(packagePath.split("/"));
        //?} else
        /*Path mixinPackagePath = modFile.findResource(packagePath);*/
        if (!Files.exists(mixinPackagePath)) {
            return List.of();
        }
        return MixinClassValidator.scanMixinFolder(mixinPackagePath.toAbsolutePath());
        //?}
    }

    @Override
    public Distribution getDistribution() {
        //? if >=1.21.11 {
        /*var dist = FMLLoader.getCurrent().getDist();
        *///?} else {
        var dist = FMLLoader.getDist();
        //?}
        return dist.isClient() ? Distribution.CLIENT : Distribution.SERVER;
    }

    @Override
    public boolean isLoadingNormally() {
        return PlatformUtil.isLoadValid();
    }

    public List<String> getLoadedModIds() {
        return LoadingModList.get().getMods().stream().map(ModInfo::getModId).toList();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return PlatformUtil.modPresent(modId);
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
