package org.embeddedt.embeddium.impl.loader.common;

//? if fabric
/*import org.embeddedt.embeddium.impl.loader.fabric.FabricEarlyLoaderServices;*/
//? if forgelike
import org.embeddedt.embeddium.impl.loader.forge.FMLEarlyLoaderServices;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

public interface EarlyLoaderServices {
    EarlyLoaderServices INSTANCE = /*? if forgelike {*/ new FMLEarlyLoaderServices() /*?} else {*/ /*new FabricEarlyLoaderServices() *//*?}*/;

    Path findEarlyMixinFolder(String path);

    Distribution getDistribution();

    boolean isLoadingNormally();

    List<String> getLoadedModIds();

    boolean isModLoaded(String modId);

    void readModMixinConfigOverrides(Consumer<MixinConfigOverride> consumer);

    record MixinConfigOverride(String modId, String key, boolean value) {}
}
