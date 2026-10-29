package net.irisshaders.iris.pipeline.programs;

import net.minecraft.client.renderer.ShaderInstance;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * The gbuffer programs of a pipeline, by {@link ShaderKey}.
 */
public class ShaderMap {
	private final GbufferProgram[] programs;

	/**
	 * Builds the program the factory creates for each key. Each is compiled as soon as its sources are transformed,
	 * and none is waited for until all are compiling, so that the driver can work on them together. If any fails,
	 * the others are deleted and the failure is thrown.
	 */
	public ShaderMap(ShaderFactory factory) {
		ShaderKey[] keys = ShaderKey.values();
		PendingGbufferProgram[] pending = new PendingGbufferProgram[keys.length];

		this.programs = new GbufferProgram[keys.length];

		try {
			LinkedBlockingQueue<PendingGbufferProgram> transformed = new LinkedBlockingQueue<>();
			int count = 0;

			for (int i = 0; i < keys.length; i++) {
				PendingGbufferProgram program = factory.create(keys[i]);

				if (program != null) {
					pending[i] = program;
					program.whenTransformed(() -> transformed.add(program));
					count++;
				}
			}

			for (int i = 0; i < count; i++) {
				transformed.take().submit();
			}

			for (int i = 0; i < keys.length; i++) {
				if (pending[i] != null) {
					programs[i] = pending[i].finish();
					pending[i] = null;
				}
			}
		} catch (InterruptedException e) {
			destroy(pending);
			throw new RuntimeException(e);
		} catch (RuntimeException | Error e) {
			destroy(pending);
			throw e;
		}
	}

	private void destroy(PendingGbufferProgram[] pending) {
		for (PendingGbufferProgram program : pending) {
			if (program != null) {
				program.delete();
			}
		}

		destroy();
	}

	public interface ShaderFactory {
		/**
		 * Returns the program for the key, or null if the pipeline has none.
		 */
		@Nullable
		PendingGbufferProgram create(ShaderKey key);
	}

	@Nullable
	public GbufferProgram getProgram(ShaderKey key) {
		return programs[key.ordinal()];
	}

	/**
	 * Returns the shader vanilla should draw with for the key, or null to draw with vanilla's own.
	 */
	@Nullable
	public ShaderInstance getShader(ShaderKey key) {
		return programs[key.ordinal()] != null ? IrisShaderInstance.get(key) : null;
	}

	public void destroy() {
		for (GbufferProgram program : programs) {
			if (program != null) {
				program.destroy();
			}
		}
	}
}
