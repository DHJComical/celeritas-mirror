package net.irisshaders.iris.compat.flywheel.compile.core;

import dev.engine_room.flywheel.backend.gl.GlObject;
import dev.engine_room.flywheel.backend.gl.shader.GlProgram;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class ClrwlCompilationHarness<K, P extends GlProgram>
{
	private final ClrwlShaderSources sources;
	private final KeyCompiler<K, P> compiler;
	private final ClrwlShaderCache shaderCache;
	private final ClrwlProgramLinker<K, P> programLinker;

	private final Map<K, P> programs = new HashMap<>();

	public ClrwlCompilationHarness(String marker, ClrwlShaderSources sources, KeyCompiler<K, P> compiler, BiFunction<K, Integer, P> programInit)
	{
		this.sources = sources;
		this.compiler = compiler;
		shaderCache = new ClrwlShaderCache();
		programLinker = new ClrwlProgramLinker<>(programInit);
	}

	public P get(K key)
	{
		return programs.computeIfAbsent(key, this::compile);
	}

	private P compile(K key)
	{
		return compiler.compile(key, sources, shaderCache, programLinker);
	}

	public void delete()
	{
		shaderCache.delete();

		programs.values()
				.forEach(GlObject::delete);

		programs.clear();
	}

	public interface KeyCompiler<K, P extends GlProgram>
	{
		P compile(K key, ClrwlShaderSources sources, ClrwlShaderCache shaderCache, ClrwlProgramLinker<K, P> programLinker);
	}
}
