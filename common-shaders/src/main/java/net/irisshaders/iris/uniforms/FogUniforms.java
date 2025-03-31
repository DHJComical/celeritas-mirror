package net.irisshaders.iris.uniforms;

import static com.mitchej123.glsm.RenderSystemService.RENDER_SYSTEM;
import static net.irisshaders.iris.gl.uniform.UniformUpdateFrequency.PER_FRAME;


import net.irisshaders.iris.gl.state.FogMode;
import net.irisshaders.iris.gl.state.StateUpdateNotifiers;
import net.irisshaders.iris.gl.uniform.DynamicUniformHolder;
import net.irisshaders.iris.gl.uniform.UniformUpdateFrequency;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

public class FogUniforms {
	private FogUniforms() {
		// no construction
	}

	public static void addFogUniforms(DynamicUniformHolder uniforms, FogMode fogMode) {
		if (fogMode == FogMode.OFF) {
			uniforms.uniform1i(UniformUpdateFrequency.ONCE, "fogMode", () -> 0);
			uniforms.uniform1i(UniformUpdateFrequency.ONCE, "fogShape", () -> -1);
		} else if (fogMode == FogMode.PER_VERTEX || fogMode == FogMode.PER_FRAGMENT) {
			uniforms.uniform1i("fogMode", () -> {
				float fogDensity = CapturedRenderingState.INSTANCE.getFogDensity();

				if (fogDensity < 0.0F) {
					return GL11.GL_LINEAR;
				} else {
					return GL11.GL_EXP2;
				}
			}, listener -> {
			});

			// To keep a stable interface, 0 is defined as spherical while 1 is defined as cylindrical, even if Mojang's index changes.
			uniforms.uniform1i(PER_FRAME, "fogShape", RENDER_SYSTEM::getFogShape);
		}

		uniforms.uniform1f("fogDensity", () -> {
			// ensure that the minimum value is 0.0
			return Math.max(0.0F, CapturedRenderingState.INSTANCE.getFogDensity());
		}, notifier -> {
		});

		uniforms.uniform1f("fogStart", RENDER_SYSTEM::getShaderFogStart, listener -> StateUpdateNotifiers.fogStartNotifier.setListener(listener));

		uniforms.uniform1f("fogEnd", RENDER_SYSTEM::getShaderFogEnd, listener -> StateUpdateNotifiers.fogEndNotifier.setListener(listener));

		uniforms
			// TODO: Update frequency of continuous?
			.uniform3f(PER_FRAME, "fogColor", () -> {
				float[] fogColor = RENDER_SYSTEM.getShaderFogColor();
				return new Vector3f(fogColor[0], fogColor[1], fogColor[2]);
			});
	}
}
