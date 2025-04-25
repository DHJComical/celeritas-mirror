package net.irisshaders.iris;

import net.irisshaders.iris.features.FeatureFlags;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.shaderpack.materialmap.BlockEntry;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import org.apache.commons.lang3.StringUtils;
import org.embeddedt.embeddium.compat.iris.IBlockEntry;
import org.jetbrains.annotations.NotNull;
import org.taumc.celeritas.CeleritasShaderVersionService;


import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

import static net.irisshaders.iris.IrisLogging.IRIS_LOGGER;

public class IrisArchaic implements CeleritasShaderVersionService {
    @Override
    public void reloadIris() throws IOException {

    }

    @Override
    public boolean irisAllowConcurrentUpdate() {
        return false;
    }

    @Override
    public void handleUnsupportedFeatureFlags(List<FeatureFlags> invalidFlagList, List<String> invalidFeatureFlags) {
        // TODO: Implement this method to handle unsupported feature flags on 1.7.10
        /*
        if (Minecraft.getInstance().screen instanceof ShaderPackScreen) {
            MutableComponent component = Component.translatable("iris.unsupported.pack.description", FeatureFlags.getInvalidStatus(invalidFlagList), invalidFeatureFlags.stream()
                    .collect(Collectors.joining(", ", ": ", ".")));
            if (SystemUtils.IS_OS_MAC) {
                component = component.append(Component.translatable("iris.unsupported.pack.macos"));
            }
            Minecraft.getInstance().setScreen(new FeatureMissingErrorScreen(Minecraft.getInstance().screen, Component.translatable("iris.unsupported.pack"), component));
        }
         */
    }

    @Override
    public void processBiomeMap(BiConsumer<String, String> define) {

    }

    @Override
    public IBlockEntry parseBlockEntry(@NotNull String entry) {
        if (entry.isEmpty()) {
            throw new IllegalArgumentException("Called BlockEntry::parse with an empty string");
        }

        // We can assume that this array is of at least array length because the input string is non-empty.
        final String[] splitStates = entry.split(":");

        // Trivial case: no states, no namespace
        if (splitStates.length == 1) {
            return new BlockEntry(new NamespacedId("minecraft", entry), Collections.emptySet());
        }

        // Examples of what we'll accept
        // stone
        // stone:0
        // minecraft:stone:0
        // minecraft:stone:0,1,2  # Theoretically valid, but I haven't seen any examples in actual shaders

        // Examples of what we don't (Yet?) accept - Seems to be from MC 1.8+
        // minecraft:farmland:moisture=0
        // minecraft:farmland:moisture=1
        // minecraft:double_plant:half=lower
        // minecraft:double_plant:half=upper
        // minecraft:grass:snowy=true
        // minecraft:unpowered_comparator:powered=false


        // Less trivial case: no metas involved, just a namespace
        //
        // The first term MUST be a valid ResourceLocation component
        // The second term, if it is not numeric, must be a valid ResourceLocation component.
        if (splitStates.length == 2 && !StringUtils.isNumeric(splitStates[1].substring(0, 1))) {
            return new BlockEntry(new NamespacedId(splitStates[0], splitStates[1]), Collections.emptySet());
        }

        // Complex case: One or more states involved...
        final int statesStart;
        final NamespacedId id;

        if (StringUtils.isNumeric(splitStates[1].substring(0, 1))) {
            // We have an entry of the form "stone:0"
            statesStart = 1;
            id = new NamespacedId("minecraft", splitStates[0]);
        } else {
            // We have an entry of the form "minecraft:stone:0"
            statesStart = 2;
            id = new NamespacedId(splitStates[0], splitStates[1]);
        }

        final Set<Integer> metas = new HashSet<>();

        for (int index = statesStart; index < splitStates.length; index++) {
            // Parse out one or more metadata ids
            final String[] metaParts = splitStates[index].split(", ");

            for (String metaPart : metaParts) {
                try {
                    metas.add(Integer.parseInt(metaPart));
                } catch (NumberFormatException e) {
                    IRIS_LOGGER.warn("Warning: the block ID map entry \"" + entry + "\" could not be fully parsed:");
                    IRIS_LOGGER.warn("- Metadata ids must be a comma separated list of one or more integers, but "+ splitStates[index] + " is not of that form!");
                }
            }
        }

        return new BlockEntry(id, metas);
    }

    @Override
    public IBlockEntry createBlockEntry(NamespacedId id) {
        return new BlockEntry(id, Set.of());
    }

    @Override
    public void onEarlyInitialize() {
//        reloadKeybind = new KeyMapping("iris.keybind.reload", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "iris.keybinds");
//        toggleShadersKeybind = new KeyMapping("iris.keybind.toggleShaders", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, "iris.keybinds");
//        shaderpackScreenKeybind = new KeyMapping("iris.keybind.shaderPackSelection", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O, "iris.keybinds");
//        wireframeKeybind = new KeyMapping("iris.keybind.wireframe", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "iris.keybinds");


//        DHCompat.run();
    }

    @Override
    public void onRenderSystemInit() {

    }

    @Override
    public void onLoadingComplete() {

    }

    @Override
    public void destroyEverything() {
        IrisCommon.getPipelineManager().destroyPipeline();

    }

    @Override
    public WorldRenderingPipeline createPipeline(NamespacedId dimensionId) {
        if (IrisCommon.getCurrentPack().isEmpty()) {
            // Completely disables shader-based rendering
            return createVanillaRenderingPipeline();
        }


        IRIS_LOGGER.error("Not Implemented");
        // TODO: This should be reverted if a dimension change causes shaders to compile again
        IrisCommon.setFallback(true);

        return createVanillaRenderingPipeline();
    }

    @Override
    public WorldRenderingPipeline createVanillaRenderingPipeline() {
        // TODO
        return null;
    }

    @Override
    public void reload() {

    }

    @Override
    public Object getDHCompatInstance(IrisRenderingPipeline pipeline, boolean renderDHShadow)
            throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        // TODO
        return null;
    }
}
