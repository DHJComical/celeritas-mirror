package net.irisshaders.iris.shaderpack.properties;

import org.taumc.celeritas.shaders.CeleritasShaders;

import java.util.Optional;

public enum ParticleRenderingSettings {
	BEFORE,
	MIXED,
	AFTER;

	public static Optional<ParticleRenderingSettings> fromString(String name) {
		try {
			return Optional.of(ParticleRenderingSettings.valueOf(name));
		} catch (IllegalArgumentException e) {
			CeleritasShaders.logger().warn("Invalid particle rendering settings! " + name);
			return Optional.empty();
		}
	}
}
