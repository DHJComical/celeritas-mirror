package net.irisshaders.iris.gl.uniform;

import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.function.Supplier;

import static com.mitchej123.glsm.RenderSystemService.RENDER_SYSTEM;

import org.lwjgl.BufferUtils;

public class MatrixFromFloatArrayUniform extends Uniform {
	private final FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
	private final Supplier<float[]> value;
	private float[] cachedValue;

	MatrixFromFloatArrayUniform(int location, Supplier<float[]> value) {
		super(location);

		this.cachedValue = null;
		this.value = value;
	}

	@Override
	public void update() {
		float[] newValue = value.get();

		if (!Arrays.equals(newValue, cachedValue)) {
			cachedValue = Arrays.copyOf(newValue, 16);

			buffer.put(cachedValue);
			buffer.rewind();

			RENDER_SYSTEM.glUniformMatrix4(location, false, buffer);
		}
	}
}
