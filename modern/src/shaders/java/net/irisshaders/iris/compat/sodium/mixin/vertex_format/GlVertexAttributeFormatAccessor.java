package net.irisshaders.iris.compat.sodium.mixin.vertex_format;

import org.embeddedt.embeddium.impl.gl.attribute.GlVertexAttributeFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GlVertexAttributeFormat.class)
public interface GlVertexAttributeFormatAccessor {
	@Invoker(value = "<init>")
	static GlVertexAttributeFormat createGlVertexAttributeFormat(int glId, int size) {
		throw new AssertionError("accessor failure");
	}
}
