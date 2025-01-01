package net.irisshaders.iris.compat.sodium.impl.shader_overrides;

import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniform;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformBlock;

import java.util.function.IntFunction;

public interface ShaderBindingContextExt {
	<U extends GlUniform<?>> U bindUniformIfPresent(String var1, IntFunction<U> var2);

	GlUniformBlock bindUniformBlockIfPresent(String var1, int var2);
}
