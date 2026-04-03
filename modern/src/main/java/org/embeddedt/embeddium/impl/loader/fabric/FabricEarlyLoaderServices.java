package org.embeddedt.embeddium.impl.loader.fabric;

//? if fabric {

/*import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import org.embeddedt.embeddium.api.EmbeddiumConstants;
import org.embeddedt.embeddium.impl.loader.common.Distribution;
import org.embeddedt.embeddium.impl.loader.common.EarlyLoaderServices;
import org.embeddedt.embeddium.impl.mixin.SodiumMixinPlugin;
import org.embeddedt.embeddium.impl.util.MixinClassValidator;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class FabricEarlyLoaderServices implements EarlyLoaderServices {

    @Override
    public List<String> findEarlyMixinClasses(String packagePath) {
        var pathOpt = FabricLoader.getInstance().getModContainer(EmbeddiumConstants.MODID).orElseThrow().findPath(packagePath);
        Path root = null;
        if (pathOpt.isPresent()) {
            root = pathOpt.get();
        } else {
            try {
                var resource = SodiumMixinPlugin.class.getResource("/" + packagePath);
                if (resource != null) {
                    Path clPath = Path.of(resource.toURI());
                    if (Files.exists(clPath)) {
                        root = clPath;
                    }
                }
            } catch (URISyntaxException ignored) {
            }
        }
        if (root == null) {
            return Collections.emptyList();
        }
        return MixinClassValidator.scanMixinFolder(root);
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
