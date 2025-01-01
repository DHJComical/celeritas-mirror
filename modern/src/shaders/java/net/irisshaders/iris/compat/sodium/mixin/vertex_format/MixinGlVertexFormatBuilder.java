package net.irisshaders.iris.compat.sodium.mixin.vertex_format;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceLinkedOpenHashMap;
import org.embeddedt.embeddium.impl.gl.attribute.GlVertexAttribute;
import org.embeddedt.embeddium.impl.gl.attribute.GlVertexAttributeFormat;
import org.embeddedt.embeddium.impl.gl.attribute.GlVertexFormat;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GlVertexFormat.Builder.class)
public class MixinGlVertexFormatBuilder<T extends Enum<T>> {
	private static final GlVertexAttribute EMPTY
		= new GlVertexAttribute(GlVertexAttributeFormat.FLOAT, "EMPTY", 0, false, 0, 0, false);
	@Shadow
	@Final
	private int stride;
	@Shadow
	@Final
	private Reference2ReferenceLinkedOpenHashMap<T, GlVertexAttribute> attributes;

	/**
	 * @author
	 * @reason
	 */
	@Overwrite(remap = false)
	private GlVertexFormat.Builder<T> addElement(T type, GlVertexAttribute attribute) {
		if (this.attributes.put(type, attribute) != null) {
			throw new IllegalStateException("Generic attribute " + type.name() + " already defined in vertex format");
		} else {
			return (GlVertexFormat.Builder<T>) (Object) this;
		}
	}
}
