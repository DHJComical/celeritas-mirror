package org.taumc.celeritas;

import net.irisshaders.iris.features.FeatureFlags;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import org.embeddedt.embeddium.compat.iris.IBlockEntry;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.BiConsumer;

public interface CeleritasShaderVersionService {
    CeleritasShaderVersionService INSTANCE = ServiceLoader.load(CeleritasShaderVersionService.class).findFirst().orElseThrow();


    /**
     * Reload the Iris
     * @throws IOException
     */
    void reloadIris() throws IOException;

    /**
     * Checks whether Iris allows concurrent updates to the shader pack.
     *
     * @return true if Iris allows concurrent updates, false otherwise.
     */
    boolean irisAllowConcurrentUpdate();

    void handleUnsupportedFeatureFlags(List<FeatureFlags> invalidFlagList, List<String> invalidFeatureFlags);

    void processBiomeMap(BiConsumer<String, String> define);

    /**
     * Parses a block ID entry.
     *
     * @param entry The string representation of the entry. Must not be empty.
     */
    IBlockEntry parseBlockEntry(@NotNull String entry);
    IBlockEntry createBlockEntry(NamespacedId id);


    void onEarlyInitialize();
    void onRenderSystemInit();
    void onLoadingComplete();

    void destroyEverything();

    WorldRenderingPipeline createPipeline(NamespacedId dimensionId);
    WorldRenderingPipeline createVanillaRenderingPipeline();

    void reload();

    Object getDHCompatInstance(IrisRenderingPipeline pipeline, boolean renderDHShadow) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException;
}
