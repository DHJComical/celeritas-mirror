package net.irisshaders.iris.compat.flywheel.compile.core;

import net.irisshaders.iris.compat.flywheel.gl.ClrwlGlShader;
import dev.engine_room.flywheel.backend.gl.shader.GlProgram;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static org.lwjgl.opengl.GL11.GL_TRUE;
import static org.lwjgl.opengl.GL20.*;

public class ClrwlProgramLinker<K, P extends GlProgram>
{
	private final BiFunction<K, Integer, P> programInit;

	public ClrwlProgramLinker(BiFunction<K, Integer, P> programInit)
	{
		this.programInit = programInit;
	}

	public P link(K key, List<ClrwlGlShader> shaders, Consumer<P> preLink)
	{
		// this probably doesn't need caching
		return linkInternal(key, shaders, preLink).unwrap();
	}

	private ClrwlLinkResult<P> linkInternal(K key, List<ClrwlGlShader> shaders, Consumer<P> preLink)
	{
		int handle = glCreateProgram();
		var out = programInit.apply(key, handle);

		for (ClrwlGlShader shader : shaders)
		{
			glAttachShader(handle, shader.handle());
		}

		preLink.accept(out);

		glLinkProgram(handle);
		String log = glGetProgramInfoLog(handle);

		if (linkSuccessful(handle))
		{
			return ClrwlLinkResult.success(out, log);
		}
		else
		{
			out.delete();
			return ClrwlLinkResult.failure(log);
		}
	}

	private static boolean linkSuccessful(int handle)
	{
		return glGetProgrami(handle, GL_LINK_STATUS) == GL_TRUE;
	}
}
