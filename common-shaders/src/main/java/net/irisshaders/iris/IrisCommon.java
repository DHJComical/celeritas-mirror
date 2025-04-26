package net.irisshaders.iris;

import com.google.common.base.Throwables;
import lombok.Getter;
import net.irisshaders.iris.config.IrisConfig;
import net.irisshaders.iris.gl.shader.StandardMacros;
import net.irisshaders.iris.helpers.OptionalBoolean;
import net.irisshaders.iris.pipeline.PipelineManager;
import net.irisshaders.iris.shaderpack.DimensionId;
import net.irisshaders.iris.shaderpack.ShaderPack;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.irisshaders.iris.shaderpack.option.OptionSet;
import net.irisshaders.iris.shaderpack.option.Profile;
import net.irisshaders.iris.shaderpack.option.values.MutableOptionValues;
import net.irisshaders.iris.shaderpack.option.values.OptionValues;
import org.jetbrains.annotations.NotNull;
import org.taumc.celeritas.CeleritasShaderVersionService;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;
import java.util.zip.ZipError;
import java.util.zip.ZipException;

import static net.irisshaders.iris.IrisLogging.IRIS_LOGGER;
import static org.embeddedt.embeddium.compat.mc.PlatformUtilService.PLATFORM_UTIL;

public class IrisCommon {

    public static final float DEGREES_TO_RADIANS = (float)Math.PI / 180.0F;
    private static Path shaderpacksDirectory;
    private static final Map<String, String> shaderPackOptionQueue = new HashMap<>();
    @Getter private static IrisConfig irisConfig;
    private static ShaderPack currentPack;
    private static boolean initialized;
    private static boolean renderSystemInit = false;
    public static NamespacedId lastDimension = null;
    private static PipelineManager pipelineManager;
    private static Optional<Exception> storedError = Optional.empty();


    private static boolean loadPackWhenPossible = false;

    public static void loadShaderpackWhenPossible() {
        loadPackWhenPossible = true;
    }


    public static Optional<Exception> getStoredError() {
        return storedError;
    }

    public static void setStoredError(Optional<Exception> error) {
        storedError = error;
    }

    public static boolean isFallback() {
        return fallback;
    }

    public static void setFallback(boolean fallback) {
        IrisCommon.fallback = fallback;
    }

    // Flag variable used when reloading
    // Used in favor of queueDefaultShaderPackOptionValues() for resetting as the
    // behavior is more concrete and therefore is more likely to repair a user's issues
    private static boolean resetShaderPackOptions = false;
    static FileSystem zipFileSystem;
    static boolean fallback;
    static String currentPackName;

    /**
     * Called once RenderSystem#initRenderer has completed. This means that we can safely access OpenGL.
     */
    public static void onRenderSystemInit() {
        if (!initialized) {
            IRIS_LOGGER.warn("Iris::onRenderSystemInit was called, but Iris::onEarlyInitialize was not called." +
                    " Trying to avoid a crash but this is an odd state.");
            return;
        }


        renderSystemInit = true;


        CeleritasShaderVersionService.INSTANCE.onRenderSystemInit();

    }

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

        try {
            if (!Files.exists(IrisCommon.getShaderpacksDirectory())) {
                Files.createDirectories(IrisCommon.getShaderpacksDirectory());
            }
        } catch (IOException e) {
            IRIS_LOGGER.warn("Failed to create the shaderpacks directory!");
            IRIS_LOGGER.warn("", e);
        }

        irisConfig = new IrisConfig(PLATFORM_UTIL.getConfigDir().resolve(IrisConstants.MODID + "-shaders.properties"));

        try {
            IrisCommon.getIrisConfig().initialize();
        } catch (IOException e) {
            IRIS_LOGGER.error("Failed to initialize Iris configuration, default values will be used instead");
            IRIS_LOGGER.error("", e);
        }

        initialized = true;
        CeleritasShaderVersionService.INSTANCE.onEarlyInitialize();
    }
    public static void onLoadingComplete() {
        if (!initialized) {
            IRIS_LOGGER.warn("Iris::onLoadingComplete was called, but Iris::onEarlyInitialize was not called." +
                    " Trying to avoid a crash but this is an odd state.");
            return;
        }

        // Initialize the pipeline now so that we don't increase world loading time. Just going to guess that
        // the player is in the overworld.
        // See: https://github.com/IrisShaders/Iris/issues/323
        lastDimension = DimensionId.OVERWORLD;

        CeleritasShaderVersionService.INSTANCE.onLoadingComplete();
    }

    public static Path getShaderpacksDirectory() {
        if (shaderpacksDirectory == null) {
            shaderpacksDirectory = PLATFORM_UTIL.getGameDir().resolve("shaderpacks");
        }

        return shaderpacksDirectory;
    }

    public static boolean isValidShaderpack(Path pack) {
        if (Files.isDirectory(pack)) {
            // Sometimes the shaderpack directory itself can be
            // identified as a shader pack due to it containing
            // folders which contain "shaders" folders, this is
            // necessary to check against that
            if (pack.equals(getShaderpacksDirectory())) {
                return false;
            }
            try (Stream<Path> stream = Files.walk(pack)) {
                return stream
                    .filter(Files::isDirectory)
                    // Prevent a pack simply named "shaders" from being
                    // identified as a valid pack
                    .filter(path -> !path.equals(pack))
                    .anyMatch(path -> path.endsWith("shaders"));
            } catch (IOException ignored) {
                // ignored, not a valid shader pack.
                return false;
            }
        }

        if (pack.toString().endsWith(".zip")) {
            try (FileSystem zipSystem = FileSystems.newFileSystem(pack, IrisCommon.class.getClassLoader())) {
                Path root = zipSystem.getRootDirectories().iterator().next();
                try (Stream<Path> stream = Files.walk(root)) {
                    return stream
                        .filter(Files::isDirectory)
                        .anyMatch(path -> path.endsWith("shaders"));
                }
            } catch (ZipError zipError) {
                // Java 8 seems to throw a ZipError instead of a subclass of IOException
                IRIS_LOGGER.warn("The ZIP at " + pack + " is corrupt");
            } catch (IOException ignored) {
                // ignored, not a valid shader pack.
            }
        }

        return false;
    }

    public static Map<String, String> getShaderPackOptionQueue() {
		return shaderPackOptionQueue;
	}

    public static void queueShaderPackOptionsFromProfile(Profile profile) {
		getShaderPackOptionQueue().putAll(profile.optionValues);
	}

    public static void queueShaderPackOptionsFromProperties(Properties properties) {
		queueDefaultShaderPackOptionValues();

		properties.stringPropertyNames().forEach(key ->
			getShaderPackOptionQueue().put(key, properties.getProperty(key)));
	}

    // Used in favor of resetShaderPackOptions as the aforementioned requires the pack to be reloaded
	public static void queueDefaultShaderPackOptionValues() {
		clearShaderPackOptionQueue();

		getCurrentPack().ifPresent(pack -> {
			OptionSet options = pack.getShaderPackOptions().getOptionSet();
			OptionValues values = pack.getShaderPackOptions().getOptionValues();

			options.getStringOptions().forEach((key, mOpt) -> {
				if (values.getStringValue(key).isPresent()) {
					getShaderPackOptionQueue().put(key, mOpt.getOption().getDefaultValue());
				}
			});
			options.getBooleanOptions().forEach((key, mOpt) -> {
				if (values.getBooleanValue(key) != OptionalBoolean.DEFAULT) {
					getShaderPackOptionQueue().put(key, Boolean.toString(mOpt.getOption().getDefaultValue()));
				}
			});
		});
	}

    public static void clearShaderPackOptionQueue() {
		getShaderPackOptionQueue().clear();
	}

    public static void resetShaderPackOptionsOnNextReload() {
		resetShaderPackOptions = true;
	}

    public static void clearResetShaderPackOptions() {
        resetShaderPackOptions = false;
    }

    public static boolean shouldResetShaderPackOptionsOnNextReload() {
		return resetShaderPackOptions;
	}

    @NotNull
    public static Optional<ShaderPack> getCurrentPack() {
        return Optional.ofNullable(currentPack);
    }

    static Optional<Path> loadExternalZipShaderpack(Path shaderpackPath) throws IOException {
        FileSystem zipSystem = FileSystems.newFileSystem(shaderpackPath, IrisCommon.class.getClassLoader());
        IrisCommon.zipFileSystem = zipSystem;

        // Should only be one root directory for a zip shaderpack
        Path root = zipSystem.getRootDirectories().iterator().next();

        Path potentialShaderDir = zipSystem.getPath("shaders");

        // If the shaders dir was immediately found return it
        // Otherwise, manually search through each directory path until it ends with "shaders"
        if (Files.exists(potentialShaderDir)) {
            return Optional.of(potentialShaderDir);
        }

        // Sometimes shaderpacks have their shaders directory within another folder in the shaderpack
        // For example Sildurs-Vibrant-Shaders.zip/shaders
        // While other packs have Trippy-Shaderpack-master.zip/Trippy-Shaderpack-master/shaders
        // This makes it hard to determine what is the actual shaders dir
        try (Stream<Path> stream = Files.walk(root)) {
            return stream
                .filter(Files::isDirectory)
                .filter(path -> path.endsWith("shaders"))
                .findFirst();
        }
    }

    static Optional<Properties> tryReadConfigProperties(Path path) {
        Properties properties = new Properties();

        if (Files.exists(path)) {
            try (InputStream is = Files.newInputStream(path)) {
                // NB: config properties are specified to be encoded with ISO-8859-1 by OptiFine,
                //     so we don't need to do the UTF-8 workaround here.
                properties.load(is);
            } catch (IOException e) {
                // TODO: Better error handling
                return Optional.empty();
            }
        }

        return Optional.of(properties);
    }

    static void tryUpdateConfigPropertiesFile(Path path, Properties properties) {
        try {
            if (properties.isEmpty()) {
                // Delete the file or don't create it if there are no changed configs
                if (Files.exists(path)) {
                    Files.delete(path);
                }

                return;
            }

            try (OutputStream out = Files.newOutputStream(path)) {
                properties.store(out, null);
            }
        } catch (IOException e) {
            // TODO: Better error handling
        }
    }

    public static void loadShaderpack() {
        if (getIrisConfig() == null) {
            if (!initialized) {
                throw new IllegalStateException("Iris::loadShaderpack was called, but Iris::onInitializeClient wasn't" +
                    " called yet. How did this happen?");
            } else {
                throw new NullPointerException("Iris.irisConfig was null unexpectedly");
            }
        }

        if (!getIrisConfig().areShadersEnabled()) {
            IRIS_LOGGER.info("Shaders are disabled because enableShaders is set to false in iris.properties");

            setShadersDisabled();

            return;
        }

        // Attempt to load an external shaderpack if it is available
        Optional<String> externalName = getIrisConfig().getShaderPackName();

        if (externalName.isEmpty()) {
            IRIS_LOGGER.info("Shaders are disabled because no valid shaderpack is selected");

            setShadersDisabled();

            return;
        }

        if (!loadExternalShaderpack(externalName.get())) {
            IRIS_LOGGER.warn("Falling back to normal rendering without shaders because the shaderpack could not be loaded");
            setShadersDisabled();
            fallback = true;
        }
    }

    static void setShadersDisabled() {
		currentPack = null;
		fallback = false;
		currentPackName = "(off)";

		IRIS_LOGGER.info("Shaders are disabled");
	}

    @SuppressWarnings("unchecked")
	private static boolean loadExternalShaderpack(String name) {
		Path shaderPackRoot;
		Path shaderPackConfigTxt;

		try {
			shaderPackRoot = getShaderpacksDirectory().resolve(name);
			shaderPackConfigTxt = getShaderpacksDirectory().resolve(name + ".txt");
		} catch (InvalidPathException e) {
			IRIS_LOGGER.error("Failed to load the shaderpack \"{}\" because it contains invalid characters in its path", name);

			return false;
		}

		if (!isValidShaderpack(shaderPackRoot)) {
			IRIS_LOGGER.error("Pack \"{}\" is not valid! Can't load it.", name);
			return false;
		}

		Path shaderPackPath;

		if (!Files.isDirectory(shaderPackRoot) && shaderPackRoot.toString().endsWith(".zip")) {
			Optional<Path> optionalPath;

			try {
				optionalPath = loadExternalZipShaderpack(shaderPackRoot);
			} catch (FileSystemNotFoundException | NoSuchFileException e) {
				IRIS_LOGGER.error("Failed to load the shaderpack \"{}\" because it does not exist in your shaderpacks folder!", name);

				return false;
			} catch (ZipException e) {
				IRIS_LOGGER.error("The shaderpack \"{}\" appears to be corrupted, please try downloading it again!", name);

				return false;
			} catch (IOException e) {
				IRIS_LOGGER.error("Failed to load the shaderpack \"{}\"!", name);
				IRIS_LOGGER.error("", e);

				return false;
			}

			if (optionalPath.isPresent()) {
				shaderPackPath = optionalPath.get();
			} else {
				IRIS_LOGGER.error("Could not load the shaderpack \"{}\" because it appears to lack a \"shaders\" directory", name);
				return false;
			}
		} else {
			if (!Files.exists(shaderPackRoot)) {
				IRIS_LOGGER.error("Failed to load the shaderpack \"{}\" because it does not exist!", name);
				return false;
			}

			// If it's a folder-based shaderpack, just use the shaders subdirectory
			shaderPackPath = shaderPackRoot.resolve("shaders");
		}

		if (!Files.exists(shaderPackPath)) {
			IRIS_LOGGER.error("Could not load the shaderpack \"{}\" because it appears to lack a \"shaders\" directory", name);
			return false;
		}

		Map<String, String> changedConfigs = tryReadConfigProperties(shaderPackConfigTxt)
			.map(properties -> (Map<String, String>) (Object) properties)
			.orElse(new HashMap<>());

		changedConfigs.putAll(getShaderPackOptionQueue());
		clearShaderPackOptionQueue();

		if (shouldResetShaderPackOptionsOnNextReload()) {
			changedConfigs.clear();
		}
        clearResetShaderPackOptions();

		try {
			currentPack = new ShaderPack(shaderPackPath, changedConfigs, StandardMacros.createStandardEnvironmentDefines());

			MutableOptionValues changedConfigsValues = currentPack.getShaderPackOptions().getOptionValues().mutableCopy();

			// Store changed values from those currently in use by the shader pack
			Properties configsToSave = new Properties();
			changedConfigsValues.getBooleanValues().forEach((k, v) -> configsToSave.setProperty(k, Boolean.toString(v)));
			changedConfigsValues.getStringValues().forEach(configsToSave::setProperty);

			tryUpdateConfigPropertiesFile(shaderPackConfigTxt, configsToSave);
		} catch (Exception e) {
			IRIS_LOGGER.error("Failed to load the shaderpack \"{}\"!", name);
			IRIS_LOGGER.error("", e);

			return false;
		}

		fallback = false;
		currentPackName = name;

		IRIS_LOGGER.info("Using shaderpack: " + name);

		return true;
	}

    /**
     * Destroys and deallocates all created OpenGL resources. Useful as part of a reload.
     */
    static void destroyEverything() {
        currentPack = null;

        CeleritasShaderVersionService.INSTANCE.destroyEverything();

        // Close the zip filesystem that the shaderpack was loaded from
        //
        // This prevents a FileSystemAlreadyExistsException when reloading shaderpacks.
        if (zipFileSystem != null) {
            try {
                zipFileSystem.close();
            } catch (NoSuchFileException e) {
                IRIS_LOGGER.warn("Failed to close the shaderpack zip when reloading because it was deleted, proceeding anyways.");
            } catch (IOException e) {
                IRIS_LOGGER.error("Failed to close zip file system?", e);
            }
        }
    }


    @NotNull
    public static PipelineManager getPipelineManager() {
        if (pipelineManager == null) {
            pipelineManager = new PipelineManager(CeleritasShaderVersionService.INSTANCE::createPipeline);
        }

        if (loadPackWhenPossible && renderSystemInit) {
            loadPackWhenPossible = false;
            CeleritasShaderVersionService.INSTANCE.reload();

        }

        return pipelineManager;
    }

    public static NamespacedId getLastDimension() {
        return lastDimension;
    }
}
