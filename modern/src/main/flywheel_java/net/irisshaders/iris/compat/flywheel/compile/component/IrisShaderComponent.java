package net.irisshaders.iris.compat.flywheel.compile.component;

import dev.engine_room.flywheel.backend.glsl.SourceComponent;

import java.util.Collection;
import java.util.List;

public record IrisShaderComponent(String name, String source) implements SourceComponent
{
	@Override
	public Collection<? extends SourceComponent> included() {
		return List.of();
	}
}
