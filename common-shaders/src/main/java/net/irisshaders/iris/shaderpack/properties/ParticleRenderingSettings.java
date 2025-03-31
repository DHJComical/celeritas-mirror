package net.irisshaders.iris.shaderpack.properties;

import java.util.Optional;

import static net.irisshaders.iris.IrisLogging.IRIS_LOGGER;

public enum ParticleRenderingSettings {
	BEFORE,
	MIXED,
	AFTER;

	public static Optional<ParticleRenderingSettings> fromString(String name) {
		try {
			return Optional.of(ParticleRenderingSettings.valueOf(name));
		} catch (IllegalArgumentException e) {
			IRIS_LOGGER.warn("Invalid particle rendering settings! " + name);
			return Optional.empty();
		}
	}
}
