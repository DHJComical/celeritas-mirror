package net.irisshaders.iris.compat.flywheel.compile.core;

import net.irisshaders.iris.compat.flywheel.gl.ClrwlGlShader;
import dev.engine_room.flywheel.backend.compile.core.FailedCompilation;
import dev.engine_room.flywheel.backend.compile.core.ShaderException;

public sealed interface ClrwlShaderResult
{
	ClrwlGlShader unwrap();

	record Success(ClrwlGlShader shader, String infoLog) implements ClrwlShaderResult
	{
		@Override
		public ClrwlGlShader unwrap() {
			return shader;
		}
	}

	record Failure(FailedCompilation failure) implements ClrwlShaderResult
	{
		@Override
		public ClrwlGlShader unwrap() {
			throw new ShaderException.Compile(failure.generateMessage());
		}
	}

	static ClrwlShaderResult success(ClrwlGlShader program, String infoLog) {
		return new Success(program, infoLog);
	}

	static ClrwlShaderResult failure(FailedCompilation failure) {
		return new Failure(failure);
	}
}
