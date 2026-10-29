package net.irisshaders.iris.pipeline;

import net.irisshaders.iris.Iris;
import org.taumc.celeritas.shaders.CeleritasShaders;

import java.util.concurrent.atomic.LongAdder;

/**
 * Collects how long each stage of loading a shader pack takes, so that the total, or a breakdown when debug options
 * are enabled, can be logged once the work is done. Stages may be recorded from any thread; several records of the
 * same stage add up.
 */
public final class ShaderLoadTimings {
	public enum Stage {
		PACK_PARSING("Shader pack parsing", false),
		PREPROCESSING("GLSL includes and preprocessing (part of pack parsing and program set)", true),
		PROGRAM_SET("Program set for dimension", false),
		PIPELINE_SETUP("Pipeline setup before compiling", false),
		COMPOSITE_SUBMIT("Waiting for composite transforms and submitting them", false),
		COMPOSITE_BUILD("Building composite and compute passes", false),
		GBUFFER_BUILD("Building gbuffer programs", false),
		EXTENDED_SHADER("Gbuffer ShaderInstance construction, including compile and link (part of the above)", true),
		PIPELINE_REST("Rest of pipeline construction", false),
		RENDERER_RELOAD("Level renderer reload", false),
		TERRAIN_BUILD("Building terrain programs", false),
		TRANSFORM("Transforming, summed across threads", true),
		COMPILE_WAIT("Waiting for submitted programs to compile and link", true);

		private final String label;
		// Overlaps other stages, so it is left out of the total.
		private final boolean overlapping;
		private final LongAdder nanos = new LongAdder();
		private final LongAdder count = new LongAdder();

		Stage(String label, boolean overlapping) {
			this.label = label;
			this.overlapping = overlapping;
		}
	}

	private ShaderLoadTimings() {
	}

	public static long now() {
		return System.nanoTime();
	}

	/**
	 * Adds the time since {@code startNanos} to the stage, and returns the current time so that consecutive stages can
	 * be chained.
	 */
	public static long record(Stage stage, long startNanos) {
		long now = System.nanoTime();
		stage.nanos.add(now - startNanos);
		stage.count.increment();
		return now;
	}

	/**
	 * Logs every stage recorded since the last report, then clears them.
	 */
	public static void report(String title) {
		StringBuilder breakdown = new StringBuilder();
		long totalNanos = 0;
		boolean any = false;

		for (Stage stage : Stage.values()) {
			long count = stage.count.sumThenReset();
			long nanos = stage.nanos.sumThenReset();

			if (count == 0) {
				continue;
			}

			any = true;
			breakdown.append("\n  ").append(stage.label).append(": ").append(nanos / 1_000_000).append(" ms");

			if (stage.overlapping) {
				breakdown.append(" (").append(count).append(count == 1 ? " time)" : " times)");
			} else {
				totalNanos += nanos;
			}
		}

		if (!any) {
			return;
		}

		String summary = "Shader loading took " + totalNanos / 1_000_000 + " ms (" + title + ")";

		if (Iris.getIrisConfig().areDebugOptionsEnabled()) {
			CeleritasShaders.logger().info(summary + ":" + breakdown);
		} else {
			CeleritasShaders.logger().info(summary);
		}
	}
}
