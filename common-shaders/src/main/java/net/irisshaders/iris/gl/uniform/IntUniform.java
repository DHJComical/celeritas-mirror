package net.irisshaders.iris.gl.uniform;

import java.util.function.IntSupplier;

import static com.mitchej123.glsm.RenderSystemService.RENDER_SYSTEM;

import net.irisshaders.iris.gl.state.ValueUpdateNotifier;

public class IntUniform extends Uniform {
	private final IntSupplier value;
	private int cachedValue;

	IntUniform(int location, IntSupplier value) {
		this(location, value, null);
	}

	IntUniform(int location, IntSupplier value, ValueUpdateNotifier notifier) {
		super(location, notifier);

		this.cachedValue = 0;
		this.value = value;
	}

	@Override
	public void update() {
		updateValue();

		if (notifier != null) {
			notifier.setListener(this::updateValue);
		}
	}

	private void updateValue() {
		int newValue = value.getAsInt();

		if (cachedValue != newValue) {
			cachedValue = newValue;
			RENDER_SYSTEM.glUniform1i(location, newValue);
		}
	}
}
