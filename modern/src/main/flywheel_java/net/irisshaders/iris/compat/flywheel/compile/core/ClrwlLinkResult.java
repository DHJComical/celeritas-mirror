package net.irisshaders.iris.compat.flywheel.compile.core;

import dev.engine_room.flywheel.backend.compile.core.ShaderException;
import dev.engine_room.flywheel.backend.gl.shader.GlProgram;
import org.jetbrains.annotations.NotNull;

public sealed interface ClrwlLinkResult<P extends GlProgram>
{
	P unwrap();

	record Success<P extends GlProgram>(P program, String log) implements ClrwlLinkResult<P>
	{
		@Override
		@NotNull
		public P unwrap() {
			return program;
		}
	}

	record Failure<P extends GlProgram>(String failure) implements ClrwlLinkResult<P>
	{
		@Override
		public P unwrap() {
			throw new ShaderException.Link(failure);
		}
	}

	static <P extends GlProgram> ClrwlLinkResult<P> success(P program, String log)
	{
		return new Success<>(program, log);
	}

	static <P extends GlProgram> ClrwlLinkResult<P> failure(String failure)
	{
		return new Failure<>(failure);
	}
}
