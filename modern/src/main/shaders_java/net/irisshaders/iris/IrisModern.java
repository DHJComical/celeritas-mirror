package net.irisshaders.iris;

//? if fabric
/*import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;*/

import com.google.common.base.Throwables;
import com.mojang.blaze3d.platform.InputConstants;
import net.irisshaders.iris.compat.dh.DHCompat;
import net.irisshaders.iris.features.FeatureFlags;
import net.irisshaders.iris.gl.shader.ShaderCompileException;
import net.irisshaders.iris.gui.FeatureMissingErrorScreen;
import net.irisshaders.iris.gui.debug.DebugLoadFailedGridScreen;
import net.irisshaders.iris.gui.screen.ShaderPackScreen;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.pipeline.ModernIrisRenderingPipeline;
import net.irisshaders.iris.pipeline.ModernVanillaRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.shaderpack.materialmap.BlockEntry;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.irisshaders.iris.texture.pbr.PBRTextureManager;
import net.irisshaders.iris.uniforms.ModernBiomeUniforms;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.apache.commons.lang3.SystemUtils;
import org.embeddedt.embeddium.compat.iris.IBlockEntry;
import org.embeddedt.embeddium.impl.loader.common.EarlyLoaderServices;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import org.taumc.celeritas.CeleritasShaderVersionService;
import org.taumc.celeritas.api.v0.CeleritasShadersApi;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static net.irisshaders.iris.IrisLogging.IRIS_LOGGER;
import static net.irisshaders.iris.uniforms.BiomeUniforms.BIOME_UNIFORMS;

public class IrisModern implements CeleritasShaderVersionService {

    public static KeyMapping reloadKeybind;
    public static KeyMapping toggleShadersKeybind;
    public static KeyMapping shaderpackScreenKeybind;
    public static KeyMapping wireframeKeybind;



    @Override
    public void reloadIris() throws IOException {
        Iris.reload();
    }

    @Override
    public boolean irisAllowConcurrentUpdate() {
        return Iris.getPipelineManager().getPipeline().map(WorldRenderingPipeline::allowConcurrentCompute).orElse(false);
    }

    @Override
    public void handleUnsupportedFeatureFlags(List<FeatureFlags> invalidFlagList, List<String> invalidFeatureFlags) {
        if (Minecraft.getInstance().screen instanceof ShaderPackScreen) {
            MutableComponent component = Component.translatable("iris.unsupported.pack.description", FeatureFlags.getInvalidStatus(invalidFlagList), invalidFeatureFlags.stream()
                    .collect(Collectors.joining(", ", ": ", ".")));
            if (SystemUtils.IS_OS_MAC) {
                component = component.append(Component.translatable("iris.unsupported.pack.macos"));
            }
            Minecraft.getInstance().setScreen(new FeatureMissingErrorScreen(Minecraft.getInstance().screen, Component.translatable("iris.unsupported.pack"), component));
        }
    }

    @Override
    public void processBiomeMap(BiConsumer<String, String> define) {
        ((ModernBiomeUniforms)BIOME_UNIFORMS).getBiomeMap().forEach((biome, id) ->
                define.accept("BIOME_" + biome.location().getPath().toUpperCase(Locale.ROOT), String.valueOf(id))
        );
    }

    @NotNull
    @Override
    public IBlockEntry parseBlockEntry(@NotNull String entry) {
        if (entry.isEmpty()) {
            throw new IllegalArgumentException("Called BlockEntry::parse with an empty string");
        }

        boolean isTag;

        if (entry.startsWith("%")) {
            isTag = true;
            entry = entry.substring(1);
        } else {
            isTag = false;
        }

        // We can assume that this array is of at least array length because the input string is non-empty.
        String[] splitStates = entry.split(":");

        // Trivial case: no states, no namespace
        if (splitStates.length == 1) {
            return new BlockEntry(new NamespacedId("minecraft", entry), Collections.emptyMap(), isTag);
        }

        // Less trivial case: no states involved, just a namespace
        //
        // The first term MUST be a valid ResourceLocation component without an equals sign
        // The second term, if it does not contain an equals sign, must be a valid ResourceLocation component.
        if (splitStates.length == 2 && !splitStates[1].contains("=")) {
            return new BlockEntry(new NamespacedId(splitStates[0], splitStates[1]), Collections.emptyMap(), isTag);
        }

        // Complex case: One or more states involved...
        int statesStart;
        NamespacedId id;

        if (splitStates[1].contains("=")) {
            // We have an entry of the form "tall_grass:half=upper"
            statesStart = 1;
            id = new NamespacedId("minecraft", splitStates[0]);
        } else {
            // We have an entry of the form "minecraft:tall_grass:half=upper"
            statesStart = 2;
            id = new NamespacedId(splitStates[0], splitStates[1]);
        }

        // We must parse each property key=value pair from the state entry.
        //
        // These pairs act as a filter on the block states. Thus, the shader pack does not have to specify all the
        // individual block properties itself; rather, it only specifies the parts of the block state that it wishes
        // to filter in/out.
        //
        // For example, some shader packs may make it so that hanging lantern blocks wave. They will put something of
        // the form "lantern:hanging=false" in the ID map as a result. Note, however, that there are also waterlogged
        // hanging lanterns, which would have "lantern:hanging=false:waterlogged=true". We must make sure that when the
        // shader pack author writes "lantern:hanging=false", that we do not just match that individual state, but that
        // we also match the waterlogged variant too.
        Map<String, String> map = new HashMap<>();

        for (int index = statesStart; index < splitStates.length; index++) {
            // Split "key=value" into the key and value
            String[] propertyParts = splitStates[index].split("=");

            if (propertyParts.length != 2) {
                IRIS_LOGGER.warn("Warning: the block ID map entry \"" + entry + "\" could not be fully parsed:");
                IRIS_LOGGER.warn("- Block state property filters must be of the form \"key=value\", but "
                        + splitStates[index] + " is not of that form!");

                // Continue and ignore the invalid entry.
                continue;
            }

            map.put(propertyParts[0], propertyParts[1]);
        }

        return new BlockEntry(id, map, isTag);
    }

    @Override
    public IBlockEntry createBlockEntry(NamespacedId id) {
        return new BlockEntry(id, Collections.emptyMap(), false);
    }

    @Override
    public void onEarlyInitialize() {
        reloadKeybind = new KeyMapping("iris.keybind.reload", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "iris.keybinds");
        toggleShadersKeybind = new KeyMapping("iris.keybind.toggleShaders", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, "iris.keybinds");
        shaderpackScreenKeybind = new KeyMapping("iris.keybind.shaderPackSelection", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O, "iris.keybinds");
        wireframeKeybind = new KeyMapping("iris.keybind.wireframe", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "iris.keybinds");

        //? if fabric {
        /*KeyBindingHelper.registerKeyBinding(reloadKeybind);
        KeyBindingHelper.registerKeyBinding(shaderpackScreenKeybind);
        KeyBindingHelper.registerKeyBinding(toggleShadersKeybind);
        KeyBindingHelper.registerKeyBinding(wireframeKeybind);
        *///?}

        DHCompat.run();

    }

    @Override
    public void onRenderSystemInit() {

        PBRTextureManager.INSTANCE.init();


        // Only load the shader pack when we can access OpenGL
        if (!EarlyLoaderServices.INSTANCE.isModLoaded("distanthorizons")) {
            IrisCommon.loadShaderpack();
        }

    }

    @Override
    public void onLoadingComplete() {

    }

    @Override
    public void destroyEverything() {
        Iris.getPipelineManager().destroyPipeline();
    }

    @Override
    public WorldRenderingPipeline createPipeline(NamespacedId dimensionId) {
        if (IrisCommon.getCurrentPack().isEmpty()) {
            // Completely disables shader-based rendering
            return createVanillaRenderingPipeline();
        }

        ProgramSet programs = IrisCommon.getCurrentPack().get().getProgramSet(dimensionId);

        // We use DeferredWorldRenderingPipeline on 1.16, and NewWorldRendering pipeline on 1.17 when rendering shaders.
        try {
            return new ModernIrisRenderingPipeline(programs);
        } catch (Exception e) {
            if (IrisCommon.getIrisConfig().areDebugOptionsEnabled()) {
                Minecraft.getInstance().setScreen(new DebugLoadFailedGridScreen(Minecraft.getInstance().screen, Component.literal(e instanceof ShaderCompileException ? "Failed to compile shaders" : "Exception"), e));
            } else {
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.displayClientMessage(Component.translatable(e instanceof ShaderCompileException ? "iris.load.failure.shader" : "iris.load.failure.generic").append(Component.literal("Copy Info").withStyle(arg -> arg.withUnderlined(true).withColor(ChatFormatting.BLUE).withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, e.getMessage())))), false);
                } else {
                    IrisCommon.setStoredError(Optional.of(e));
                }
            }
            IRIS_LOGGER.error("Failed to create shader rendering pipeline, disabling shaders!", e);
            // TODO: This should be reverted if a dimension change causes shaders to compile again
            IrisCommon.setFallback(true);

            return createVanillaRenderingPipeline();
        }
    }

    @Override
    public WorldRenderingPipeline createVanillaRenderingPipeline() {
        return new ModernVanillaRenderingPipeline();
    }

    @Override
    public void reload() {
        try {
            Iris.reload();
        } catch (IOException e) {
            IRIS_LOGGER.error("Error while reloading Shaders for " + IrisConstants.MODNAME + "!", e);

            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(Component.translatable("iris.shaders.reloaded.failure", Throwables.getRootCause(e).getMessage()).withStyle(ChatFormatting.RED), false);
            }
        }
    }

    @Override
    public Object getDHCompatInstance(IrisRenderingPipeline pipeline, boolean renderDHShadow) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        return Class.forName("net.irisshaders.iris.compat.dh.DHCompatInternal").getDeclaredConstructor(ModernIrisRenderingPipeline.class, boolean.class).newInstance((ModernIrisRenderingPipeline)pipeline, renderDHShadow);
    }
}
