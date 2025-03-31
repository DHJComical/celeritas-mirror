package net.irisshaders.iris.uniforms;

import net.irisshaders.iris.gl.uniform.DynamicUniformHolder;
import org.joml.Vector2f;

import static com.mitchej123.glsm.GLStateManagerService.GL_STATE_MANAGER;
import static com.mitchej123.glsm.RenderSystemService.RENDER_SYSTEM;

public class VanillaUniforms {
	public static void addVanillaUniforms(DynamicUniformHolder uniforms) {
		Vector2f cachedScreenSize = new Vector2f();
		// listener -> {} dictates we want this to run on every shader update, not just on a new frame. These are dynamic.
		uniforms.uniform1f("iris_LineWidth", RENDER_SYSTEM::getShaderLineWidth, listener -> {
		});
		uniforms.uniform2f("iris_ScreenSize", () -> cachedScreenSize.set(GL_STATE_MANAGER.getViewportWidth(), GL_STATE_MANAGER.getViewportHeight()), listener -> {
		});
	}
}
