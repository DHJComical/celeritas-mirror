package net.irisshaders.iris.uniforms;

import net.irisshaders.iris.gl.uniform.UniformHolder;

import static net.irisshaders.iris.gl.uniform.UniformUpdateFrequency.PER_TICK;
import static org.embeddedt.embeddium.compat.mc.MinecraftVersionShimService.MINECRAFT_SHIM;

public final class WorldTimeUniforms {
	private WorldTimeUniforms() {
	}

	/**
	 * Makes world time uniforms available to the given program
	 *
	 * @param uniforms the program to make the uniforms available to
	 */
	public static void addWorldTimeUniforms(UniformHolder uniforms) {
		uniforms
			.uniform1i(PER_TICK, "worldTime", WorldTimeUniforms::getWorldDayTime)
			.uniform1i(PER_TICK, "worldDay", WorldTimeUniforms::getWorldDay)
			.uniform1i(PER_TICK, "moonPhase", MINECRAFT_SHIM::getMoonPhase);
	}

	static int getWorldDayTime() {
		long timeOfDay = MINECRAFT_SHIM.getDayTime();

		if (MINECRAFT_SHIM.isCurrentDimensionEnd() || MINECRAFT_SHIM.isCurrentDimensionNether()) {
			// If the dimension is the nether or the end, don't override the fixed time.
			// This was an oversight in versions before and including 1.2.5 causing inconsistencies, such as Complementary's ender beams not moving.
			return (int) (timeOfDay % 24000L);
		}

		long dayTime = MINECRAFT_SHIM.getDimensionTime(timeOfDay % 24000L);

		return (int) dayTime;
	}

	private static int getWorldDay() {
		long timeOfDay = MINECRAFT_SHIM.getDayTime();
		long day = timeOfDay / 24000L;

		return (int) day;
	}

}
