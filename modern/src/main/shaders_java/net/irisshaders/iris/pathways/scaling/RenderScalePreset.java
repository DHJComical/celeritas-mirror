package net.irisshaders.iris.pathways.scaling;

public enum RenderScalePreset {
	NATIVE(100),
	ULTRA_QUALITY(77),
	QUALITY(67),
	BALANCED(59),
	PERFORMANCE(50);

	private final int percent;

	RenderScalePreset(int percent) {
		this.percent = percent;
	}

	public int getPercent() {
		return percent;
	}

	public float getScale() {
		return percent / 100.0f;
	}

	public boolean isScaled() {
		return this != NATIVE;
	}
}
