package net.irisshaders.iris.compat.sodium.impl.shader_overrides;

import com.mojang.blaze3d.systems.RenderSystem;
import org.embeddedt.embeddium.impl.gl.shader.ShaderBindingContext;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformFloat;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformFloat4v;

public class IrisShaderFogComponent {
	private final GlUniformFloat4v uFogColor;
	private final GlUniformFloat uFogStart;
	private final GlUniformFloat uFogEnd;

	public IrisShaderFogComponent(ShaderBindingContext context) {
		this.uFogColor = context.bindUniformIfPresent("iris_FogColor", GlUniformFloat4v::new);
		this.uFogStart = context.bindUniformIfPresent("iris_FogStart", GlUniformFloat::new);
		this.uFogEnd = context.bindUniformIfPresent("iris_FogEnd", GlUniformFloat::new);
	}

	public void setup() {
		if (this.uFogColor != null) {
			this.uFogColor.set(RenderSystem.getShaderFogColor());
		}

		if (this.uFogStart != null) {
			this.uFogStart.setFloat(RenderSystem.getShaderFogStart());
		}

		if (this.uFogEnd != null) {
			this.uFogEnd.setFloat(RenderSystem.getShaderFogEnd());
		}
	}
}
