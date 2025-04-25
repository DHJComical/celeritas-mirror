package net.irisshaders.iris;

import com.google.common.base.Throwables;
import com.mojang.blaze3d.platform.GlDebug;
//? if fabric
/*import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;*/
import net.irisshaders.iris.compat.dh.DHCompat;
import net.irisshaders.iris.compat.sodium.impl.options.IrisSodiumOptions;
import net.irisshaders.iris.gui.screen.ShaderPackScreen;
import net.irisshaders.iris.pipeline.ModernIrisRenderingPipeline;
import net.irisshaders.iris.pipeline.PipelineManager;
import net.irisshaders.iris.shaderpack.DimensionId;
import net.irisshaders.iris.shaderpack.discovery.ShaderpackDirectoryManager;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.irisshaders.iris.texture.pbr.PBRTextureManager;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import org.embeddedt.embeddium.impl.Celeritas;
import org.embeddedt.embeddium.impl.gl.debug.GLDebug;
import org.embeddedt.embeddium.impl.loader.common.EarlyLoaderServices;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

import static net.irisshaders.iris.IrisLogging.IRIS_LOGGER;
import static org.embeddedt.embeddium.compat.mc.PlatformUtilService.PLATFORM_UTIL;

public class Iris {

    public static final IrisLogging logger = IRIS_LOGGER;
	// Change this for snapshots!
	private static final String backupVersionNumber = "1.20.3";
	public static boolean testing = false;
    private static ShaderpackDirectoryManager shaderpacksDirectoryManager;


	public static boolean isPackInUseQuick() {
		return IrisCommon.getPipelineManager().getPipelineNullable() instanceof ModernIrisRenderingPipeline;
	}

	/**
	 * Called once RenderSystem#initRenderer has completed. This means that we can safely access OpenGL.
	 */
	public static void onRenderSystemInit() {

		PBRTextureManager.INSTANCE.init();


		// Only load the shader pack when we can access OpenGL
		if (!EarlyLoaderServices.INSTANCE.isModLoaded("distanthorizons")) {
			IrisCommon.loadShaderpack();
		}
	}

	public static void duringRenderSystemInit() {
		setDebug(IrisCommon.getIrisConfig().areDebugOptionsEnabled());
	}

	/**
	 * Called when the title screen is initialized for the first time.
	 */
	public static void onLoadingComplete() {
		Iris.getPipelineManager().preparePipeline(DimensionId.OVERWORLD);

        IrisSodiumOptions.init();
	}

	public static void handleKeybinds(Minecraft minecraft) {
		if (IrisModern.reloadKeybind.consumeClick()) {
			try {
				reload();

				if (minecraft.player != null) {
					minecraft.player.displayClientMessage(Component.translatable("iris.shaders.reloaded"), false);
				}

			} catch (Exception e) {
				logger.error("Error while reloading Shaders for " + IrisConstants.MODNAME + "!", e);

				if (minecraft.player != null) {
					minecraft.player.displayClientMessage(Component.translatable("iris.shaders.reloaded.failure", Throwables.getRootCause(e).getMessage()).withStyle(ChatFormatting.RED), false);
				}
			}
		} else if (IrisModern.toggleShadersKeybind.consumeClick()) {
			try {
				toggleShaders(minecraft, !IrisCommon.getIrisConfig().areShadersEnabled());
			} catch (Exception e) {
				logger.error("Error while toggling shaders!", e);

				if (minecraft.player != null) {
					minecraft.player.displayClientMessage(Component.translatable("iris.shaders.toggled.failure", Throwables.getRootCause(e).getMessage()).withStyle(ChatFormatting.RED), false);
				}
				IrisCommon.setShadersDisabled();
				IrisCommon.fallback = true;
			}
		} else if (IrisModern.shaderpackScreenKeybind.consumeClick()) {
			minecraft.setScreen(new ShaderPackScreen(null));
		} else if (IrisModern.wireframeKeybind.consumeClick()) {
			if (IrisCommon.getIrisConfig().areDebugOptionsEnabled() && minecraft.player != null && !Minecraft.getInstance().isLocalServer()) {
				minecraft.player.displayClientMessage(Component.literal("No cheating; wireframe only in singleplayer!"), false);
			}
		}
	}

	public static boolean shouldActivateWireframe() {
		return IrisCommon.getIrisConfig().areDebugOptionsEnabled() && IrisModern.wireframeKeybind.isDown();
	}

	public static void toggleShaders(Minecraft minecraft, boolean enabled) throws IOException {
		IrisCommon.getIrisConfig().setShadersEnabled(enabled);
		IrisCommon.getIrisConfig().save();

		reload();
		if (minecraft.player != null) {
			minecraft.player.displayClientMessage(enabled ? Component.translatable("iris.shaders.toggled", IrisCommon.currentPackName) : Component.translatable("iris.shaders.disabled"), false);
		}
	}

    public static void setDebug(boolean enable) {
		try {
			IrisCommon.getIrisConfig().setDebugEnabled(enable);
			IrisCommon.getIrisConfig().save();
		} catch (IOException e) {
			IRIS_LOGGER.fatal("Failed to save config!", e);
		}

		int success;
		if (enable) {
			success = GLDebug.setupDebugMessageCallback();
		} else {
			GLDebug.reloadDebugState();
			GlDebug.enableDebugCallback(Minecraft.getInstance().options.glDebugVerbosity, false);
			success = 1;
		}

		logger.info("Debug functionality is " + (enable ? "enabled, logging will be more verbose!" : "disabled."));
		if (Minecraft.getInstance().player != null) {
			Minecraft.getInstance().player.displayClientMessage(Component.translatable(success != 0 ? (enable ? "iris.shaders.debug.enabled" : "iris.shaders.debug.disabled") : "iris.shaders.debug.failure"), false);
			if (success == 2) {
				Minecraft.getInstance().player.displayClientMessage(Component.translatable("iris.shaders.debug.restart"), false);
			}
		}
	}

	private static Optional<Properties> tryReadConfigProperties(Path path) {
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

    public static boolean isValidToShowPack(Path pack) {
		return Files.isDirectory(pack) || pack.toString().endsWith(".zip");
	}

    public static void reload() throws IOException {
		// allows shaderpacks to be changed at runtime
		IrisCommon.getIrisConfig().initialize();

		// Destroy all allocated resources
		IrisCommon.destroyEverything();

		// Load the new shaderpack
		IrisCommon.loadShaderpack();

		// Very important - we need to re-create the pipeline straight away.
		// https://github.com/IrisShaders/Iris/issues/1330
		if (Minecraft.getInstance().level != null) {
			Iris.getPipelineManager().preparePipeline(Iris.getCurrentDimension());
		}
	}

    public static NamespacedId getCurrentDimension() {
		ClientLevel level = Minecraft.getInstance().level;

		if (level != null) {
			return new NamespacedId(level.dimension().location().getNamespace(), level.dimension().location().getPath());
		} else {
			// This prevents us from reloading the shaderpack unless we need to. Otherwise, if the player is in the
			// nether and quits the game, we might end up reloading the shaders on exit and on entry to the level
			// because the code thinks that the dimension changed.
			return IrisCommon.getLastDimension();
		}
	}

	@NotNull
	public static PipelineManager getPipelineManager() {
		return IrisCommon.getPipelineManager();
	}

	public static Optional<Exception> getStoredError() {
		Optional<Exception> stored = IrisCommon.getStoredError();
        IrisCommon.setStoredError(Optional.empty());
		return stored;
	}

	public static String getCurrentPackName() {
		return IrisCommon.currentPackName;
	}


	public static boolean isFallback() {
		return IrisCommon.fallback;
	}

	public static String getVersion() {
		return Celeritas.getVersion();
	}

	public static String getFormattedVersion() {
		ChatFormatting color;
		String version = getVersion();

		if (PLATFORM_UTIL.isDevelopmentEnvironment()) {
			color = ChatFormatting.GOLD;
			version = version + " (Development Environment)";
		} else if (version.endsWith("-dirty") || version.contains("unknown") || version.endsWith("-nogit")) {
			color = ChatFormatting.RED;
		} else if (version.contains("+rev.")) {
			color = ChatFormatting.LIGHT_PURPLE;
		} else {
			color = ChatFormatting.GREEN;
		}

		return color + version;
	}

	/**
	 * Gets the current release target. Since 1.19.3, Mojang no longer stores this information, so we must manually provide it for snapshots.
	 *
	 * @return Release target
	 */
	public static String getReleaseTarget() {
		// If this is a snapshot, you must change backupVersionNumber!
		SharedConstants.tryDetectVersion();
		return SharedConstants.getCurrentVersion().isStable() ? SharedConstants.getCurrentVersion().getName() : backupVersionNumber;
	}

	public static String getBackupVersionNumber() {
		return backupVersionNumber;
	}

    public static ShaderpackDirectoryManager getShaderpacksDirectoryManager() {
		if (shaderpacksDirectoryManager == null) {
			shaderpacksDirectoryManager = new ShaderpackDirectoryManager(IrisCommon.getShaderpacksDirectory());
		}

		return shaderpacksDirectoryManager;
	}

	public static boolean loadedIncompatiblePack() {
		return DHCompat.lastPackIncompatible();
	}


}
