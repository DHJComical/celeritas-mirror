package net.irisshaders.iris.compat.sodium.mixin.shader_overrides;

import org.embeddedt.embeddium.impl.gl.shader.ShaderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ShaderType.class)
public interface ShaderTypeAccessor {
	@Invoker(value = "<init>")
	static ShaderType createShaderType(String name, int ordinal, int glId) {
		throw new AssertionError();
	}
}
