package net.irisshaders.iris.compat.flywheel.engine;

import net.irisshaders.iris.compat.flywheel.Colorwheel;
import dev.engine_room.flywheel.api.layout.FloatRepr;
import dev.engine_room.flywheel.api.layout.Layout;
import dev.engine_room.flywheel.api.layout.LayoutBuilder;
import dev.engine_room.flywheel.backend.LayoutAttributes;
import dev.engine_room.flywheel.backend.gl.array.VertexAttribute;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class ClrwlVertex {
	public static final Layout LAYOUT = LayoutBuilder.create()
			.vector("position", FloatRepr.FLOAT, 3)
			.vector("color", FloatRepr.NORMALIZED_UNSIGNED_BYTE, 4)
			.vector("tex", FloatRepr.FLOAT, 2)
			.vector("light", FloatRepr.UNSIGNED_SHORT, 2)
			.vector("normal", FloatRepr.NORMALIZED_BYTE, 4)
			.vector("entity", FloatRepr.SHORT, 2)
			.vector("midTexCoord", FloatRepr.FLOAT, 2)
			.vector("tangent", FloatRepr.NORMALIZED_BYTE, 4)
			.vector("midBlock", FloatRepr.BYTE, 4)
			.vector("overlay", FloatRepr.SHORT, 2)
			.build();

	public static final List<VertexAttribute> ATTRIBUTES = LayoutAttributes.attributes(LAYOUT);
	public static final int STRIDE = LAYOUT.byteSize();

	public static final ResourceLocation LAYOUT_SHADER = Colorwheel.rl("internal/vertex_input.vert");

	private ClrwlVertex() {
	}

	public static ClrwlVertexView createVertexView() {
		return new ClrwlVertexView();
	}
}
