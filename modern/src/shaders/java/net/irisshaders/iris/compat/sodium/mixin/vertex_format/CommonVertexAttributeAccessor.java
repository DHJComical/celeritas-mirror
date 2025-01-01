package net.irisshaders.iris.compat.sodium.mixin.vertex_format;

import com.mojang.blaze3d.vertex.VertexFormatElement;
import org.embeddedt.embeddium.api.vertex.attributes.CommonVertexAttribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(CommonVertexAttribute.class)
public interface CommonVertexAttributeAccessor {
	@Invoker(value = "<init>")
	static CommonVertexAttribute createCommonVertexElement(String name, int ordinal, VertexFormatElement element) {
		throw new AssertionError();
	}
}
