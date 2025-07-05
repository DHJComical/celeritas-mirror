package net.irisshaders.iris.uniforms;

import net.irisshaders.iris.gl.uniform.UniformHolder;

import static net.irisshaders.iris.gl.uniform.UniformUpdateFrequency.PER_FRAME;
import static org.embeddedt.embeddium.compat.mc.MinecraftVersionShimService.MINECRAFT_SHIM;

/**
 * Implements uniforms relating the current viewport
 *
 * @see <a href="https://github.com/IrisShaders/ShaderDoc/blob/master/uniforms.md#viewport">Uniforms: Viewport</a>
 */
public final class ViewportUniforms {
	// cannot be constructed
	private ViewportUniforms() {
	}

	/**
	 * Makes the viewport uniforms available to the given program
	 *
	 * @param uniforms the program to make the uniforms available to
	 */
	public static void addViewportUniforms(UniformHolder uniforms) {
		// TODO: What about the custom scale.composite3 property?
		// NB: It is not safe to cache the render target due to mods like Resolution Control modifying the render target field.
		uniforms
			.uniform1f(PER_FRAME, "viewHeight", MINECRAFT_SHIM::getMinecraftRenderHeight)
			.uniform1f(PER_FRAME, "viewWidth", MINECRAFT_SHIM::getMinecraftRenderWidth)
			.uniform1f(PER_FRAME, "aspectRatio", ViewportUniforms::getAspectRatio);
	}

	/**
	 * @return the current viewport aspect ratio, calculated from the current Minecraft window size
	 */
	private static float getAspectRatio() {
		return ((float) MINECRAFT_SHIM.getMinecraftRenderWidth()) / ((float) MINECRAFT_SHIM.getMinecraftRenderHeight());
	}
}
