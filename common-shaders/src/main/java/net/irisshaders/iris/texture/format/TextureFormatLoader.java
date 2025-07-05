package net.irisshaders.iris.texture.format;

import org.embeddedt.embeddium.compat.mc.MCResource;
import org.embeddedt.embeddium.compat.mc.MCResourceLocation;
import org.embeddedt.embeddium.compat.mc.MCResourceProvider;
import org.jetbrains.annotations.Nullable;
import org.taumc.celeritas.CeleritasShaderVersionService;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

import static net.irisshaders.iris.IrisLogging.IRIS_LOGGER;
import static org.embeddedt.embeddium.compat.mc.MinecraftVersionShimService.MINECRAFT_SHIM;

public class TextureFormatLoader {
	public static final MCResourceLocation LOCATION = MINECRAFT_SHIM.makeResourceLocation("optifine/texture.properties");

	private static TextureFormat format;

	@Nullable
	public static TextureFormat getFormat() {
		return format;
	}

	public static void reload(MCResourceProvider resourceManager) {
		TextureFormat newFormat = loadFormat(resourceManager);
		boolean didFormatChange = !Objects.equals(format, newFormat);
		format = newFormat;
		if (didFormatChange) {
			onFormatChange();
		}
	}

	@Nullable
	private static TextureFormat loadFormat(MCResourceProvider resourceManager) {
		Optional<MCResource> resource = resourceManager.getResource((MCResourceLocation)(Object)LOCATION);
		if (resource.isPresent()) {
			try (InputStream stream = resource.get().open()) {
				Properties properties = new Properties();
				properties.load(stream);
				String format = properties.getProperty("format");
				if (format != null && !format.isEmpty()) {
					String[] splitFormat = format.split("/");
					if (splitFormat.length > 0) {
						String name = splitFormat[0];
						TextureFormat.Factory factory = TextureFormatRegistry.INSTANCE.getFactory(name);
						if (factory != null) {
							String version;
							if (splitFormat.length > 1) {
								version = splitFormat[1];
							} else {
								version = null;
							}
							return factory.createFormat(name, version);
						} else {
							IRIS_LOGGER.warn("Invalid texture format '" + name + "' in file '" + LOCATION + "'");
						}
					}
				}
			} catch (FileNotFoundException e) {
				//
			} catch (Exception e) {
				IRIS_LOGGER.error("Failed to load texture format from file '" + LOCATION + "'", e);
			}
		}
		return null;
	}

	private static void onFormatChange() {
		try {
            CeleritasShaderVersionService.INSTANCE.reloadIris();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
