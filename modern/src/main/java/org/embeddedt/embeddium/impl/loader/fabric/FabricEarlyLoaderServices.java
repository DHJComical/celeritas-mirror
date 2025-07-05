package org.embeddedt.embeddium.impl.loader.fabric;

//? if fabric {

/*import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import org.embeddedt.embeddium.api.EmbeddiumConstants;
import org.embeddedt.embeddium.impl.loader.common.Distribution;
import org.embeddedt.embeddium.impl.loader.common.EarlyLoaderServices;
import org.embeddedt.embeddium.impl.mixin.SodiumMixinPlugin;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

public class FabricEarlyLoaderServices implements EarlyLoaderServices {

    @Override
    public Path findEarlyMixinFolder(String path) {
        var pathOpt = FabricLoader.getInstance().getModContainer(EmbeddiumConstants.MODID).orElseThrow().findPath(path);
        if(pathOpt.isPresent()) {
            return pathOpt.get();
        } else {
            try {
                var resource = SodiumMixinPlugin.class.getResource("/" + path);
                if (resource == null) {
                    return null;
                }
                Path clPath = Path.of(resource.toURI());
                if(Files.exists(clPath)) {
                    return clPath;
                }
            } catch(URISyntaxException ignored) {
            }
            return null;
        }
    }

    @Override
    public Distribution getDistribution() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT ? Distribution.CLIENT : Distribution.SERVER;
    }

    @Override
    public boolean isLoadingNormally() {
        return true;
    }

    @Override
    public List<String> getLoadedModIds() {
        return FabricLoader.getInstance().getAllMods().stream().map(m -> m.getMetadata().getId()).toList();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public void readModMixinConfigOverrides(Consumer<MixinConfigOverride> consumer) {
        // TODO
    }
}
*///?}
