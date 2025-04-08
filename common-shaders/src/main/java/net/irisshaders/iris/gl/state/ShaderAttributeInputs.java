package net.irisshaders.iris.gl.state;

public class ShaderAttributeInputs {
	private boolean color;
	private boolean tex;
	private boolean overlay;
	private boolean light;
	private boolean normal;
	private boolean newLines;
	private boolean glint;
	private boolean text;
	// WARNING: adding new fields requires updating hashCode and equals methods!

	public ShaderAttributeInputs(boolean color, boolean tex, boolean overlay, boolean light, boolean normal) {
		this.color = color;
		this.tex = tex;
		this.overlay = overlay;
		this.light = light;
		this.normal = normal;

        this.newLines = false;
        this.glint = false;
        this.text = false;
	}

    public ShaderAttributeInputs(boolean color, boolean tex, boolean overlay, boolean light, boolean normal, boolean newLines, boolean glint, boolean text) {
        this.color = color;
        this.tex = tex;
        this.overlay = overlay;
        this.light = light;
        this.normal = normal;
        this.newLines = newLines;
        this.glint = glint;
        this.text = text;
    }
	public boolean hasColor() {
		return color;
	}

	public boolean hasTex() {
		return tex;
	}

	public boolean hasOverlay() {
		return overlay;
	}

	public boolean hasLight() {
		return light;
	}

	public boolean hasNormal() {
		return normal;
	}

	public boolean isNewLines() {
		return newLines;
	}

	public boolean isGlint() {
		return glint;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (color ? 1231 : 1237);
		result = prime * result + (tex ? 1231 : 1237);
		result = prime * result + (overlay ? 1231 : 1237);
		result = prime * result + (light ? 1231 : 1237);
		result = prime * result + (normal ? 1231 : 1237);
		result = prime * result + (newLines ? 1231 : 1237);
		result = prime * result + (glint ? 1231 : 1237);
		result = prime * result + (text ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ShaderAttributeInputs other = (ShaderAttributeInputs) obj;
		if (color != other.color)
			return false;
		if (tex != other.tex)
			return false;
		if (overlay != other.overlay)
			return false;
		if (light != other.light)
			return false;
		if (normal != other.normal)
			return false;
		if (newLines != other.newLines)
			return false;
		if (glint != other.glint)
			return false;
		return text == other.text;
	}

	public boolean isText() {
		return text;
	}
}
