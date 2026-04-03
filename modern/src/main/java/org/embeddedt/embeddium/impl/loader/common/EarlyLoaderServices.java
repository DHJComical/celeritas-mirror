package org.embeddedt.embeddium.impl.loader.common;

//? if fabric
/*import org.embeddedt.embeddium.impl.loader.fabric.FabricEarlyLoaderServices;*/
//? if forgelike
import org.embeddedt.embeddium.impl.loader.forge.FMLEarlyLoaderServices;

import java.util.List;
import java.util.function.Consumer;

public interface EarlyLoaderServices {
    EarlyLoaderServices INSTANCE = /*? if forgelike {*/ new FMLEarlyLoaderServices() /*?} else {*/ /*new FabricEarlyLoaderServices() *//*?}*/;

    /**
     * Discovers all mixin class names within the given package path (slash-separated, trailing slash included).
     * Returns names relative to that package, dot-separated, without the {@code .class} suffix
     * (e.g. {@code "features.render.world.ClientLevelMixin"}).
     */
    List<String> findEarlyMixinClasses(String packagePath);

    Distribution getDistribution();

    boolean isLoadingNormally();

    List<String> getLoadedModIds();

    boolean isModLoaded(String modId);

    void readModMixinConfigOverrides(Consumer<MixinConfigOverride> consumer);

    record MixinConfigOverride(String modId, String key, boolean value) {}
}
