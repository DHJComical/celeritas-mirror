package net.irisshaders.iris;

import net.irisshaders.iris.config.IrisConfig;

import java.io.IOException;
import java.nio.file.Files;

import static org.embeddedt.embeddium.compat.mc.PlatformUtilService.PLATFORM_UTIL;

public class Iris {
    public static final IrisLogging logger = IrisLogging.IRIS_LOGGER;
    private static boolean initialized;
    private static IrisConfig irisConfig;

    /**
     * Called very early on in Minecraft initialization. At this point we *cannot* safely access OpenGL, but we can do
     * some very basic setup, config loading, and environment checks.
     *
     * <p>This is roughly equivalent to Fabric Loader's ClientModInitializer#onInitializeClient entrypoint, except
     * it's entirely cross platform & we get to decide its exact semantics.</p>
     *
     * <p>This is called right before options are loaded, so we can add key bindings here.</p>
     */
    public static void onEarlyInitialize() {
//        reloadKeybind = new KeyMapping("iris.keybind.reload", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "iris.keybinds");
//        toggleShadersKeybind = new KeyMapping("iris.keybind.toggleShaders", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, "iris.keybinds");
//        shaderpackScreenKeybind = new KeyMapping("iris.keybind.shaderPackSelection", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O, "iris.keybinds");
//        wireframeKeybind = new KeyMapping("iris.keybind.wireframe", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "iris.keybinds");


//        DHCompat.run();

        try {
            if (!Files.exists(IrisCommon.getShaderpacksDirectory())) {
                Files.createDirectories(IrisCommon.getShaderpacksDirectory());
            }
        } catch (IOException e) {
            logger.warn("Failed to create the shaderpacks directory!");
            logger.warn("", e);
        }

        irisConfig = new IrisConfig(PLATFORM_UTIL.getConfigDir().resolve(IrisConstants.MODID + "-shaders.properties"));

        try {
            irisConfig.initialize();
        } catch (IOException e) {
            logger.error("Failed to initialize Iris configuration, default values will be used instead");
            logger.error("", e);
        }

        initialized = true;
    }
}
