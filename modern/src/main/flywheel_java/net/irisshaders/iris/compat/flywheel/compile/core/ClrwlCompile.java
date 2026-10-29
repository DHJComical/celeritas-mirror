package net.irisshaders.iris.compat.flywheel.compile.core;

import net.irisshaders.iris.compat.flywheel.Colorwheel;
import net.irisshaders.iris.compat.flywheel.gl.ClrwlGlShader;
import net.irisshaders.iris.compat.flywheel.gl.ClrwlShaderType;
import dev.engine_room.flywheel.backend.gl.shader.GlProgram;
import dev.engine_room.flywheel.backend.glsl.GlslVersion;
import dev.engine_room.flywheel.backend.glsl.SourceComponent;
import dev.engine_room.flywheel.lib.util.StringUtil;
import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A typed provider for shader compiler builders.
 * <br>
 * This could just be a static utility class, but creating an instance of Compile
 * and calling the functors on it prevents you from having to specify the key type everywhere.
 * <br>
 * Consider {@code Compile.<PipelineKey>shader(...)} vs {@code PIPELINE.shader(...)}
 *
 * @param <K> The type of the key used to compile shaders.
 */
public class ClrwlCompile<K, P extends GlProgram>
{
	public ShaderCompiler<K> shader(GlslVersion glslVersion, ClrwlShaderType shaderType)
	{
		return new ShaderCompiler<>(glslVersion, shaderType);
	}

	public ProgramStitcher<K, P> program()
	{
		return new ProgramStitcher<>();
	}

	public static class ShaderCompiler<K>
	{
		private final GlslVersion glslVersion;
		private final ClrwlShaderType shaderType;
		private final List<BiFunction<K, ClrwlShaderSources, SourceComponent>> fetchers = new ArrayList<>();
		private BiConsumer<K, ClrwlCompilation> compilationCallbacks = ($, $$) -> {
		};
		private Function<K, String> nameMapper = Object::toString;
		private BiFunction<K, ClrwlShaderSources, Boolean> condition = ($, $$) -> true;

		public ShaderCompiler(GlslVersion glslVersion, ClrwlShaderType shaderType)
		{
			this.glslVersion = glslVersion;
			this.shaderType = shaderType;
		}

		public ShaderCompiler<K> nameMapper(Function<K, String> nameMapper)
		{
			this.nameMapper = nameMapper;
			return this;
		}

		public ShaderCompiler<K> condition(BiFunction<K, ClrwlShaderSources, Boolean> condition)
		{
			this.condition = condition;
			return this;
		}

		public ShaderCompiler<K> with(BiFunction<K, ClrwlShaderSources, SourceComponent> fetch)
		{
			fetchers.add(fetch);
			return this;
		}

		public ShaderCompiler<K> withComponents(Collection<SourceComponent> components)
		{
			components.forEach(this::withComponent);
			return this;
		}

		public ShaderCompiler<K> withComponent(SourceComponent component)
		{
			return withComponent($ -> component);
		}

		public ShaderCompiler<K> withComponent(Function<K, SourceComponent> sourceFetcher)
		{
			return with((key, $) -> sourceFetcher.apply(key));
		}

		public ShaderCompiler<K> withResource(Function<K, ResourceLocation> sourceFetcher)
		{
			return with((key, c) -> c.flwSources().get(sourceFetcher.apply(key)));
		}

		public ShaderCompiler<K> withResource(ResourceLocation resourceLocation)
		{
			return withResource($ -> resourceLocation);
		}

		public ShaderCompiler<K> onCompile(BiConsumer<K, ClrwlCompilation> cb)
		{
			compilationCallbacks = compilationCallbacks.andThen(cb);
			return this;
		}

		public ShaderCompiler<K> define(String def, int value)
		{
			return onCompile(($, ctx) -> ctx.define(def, String.valueOf(value)));
		}

		public ShaderCompiler<K> enableExtension(String extension)
		{
			return onCompile(($, ctx) -> ctx.enableExtension(extension));
		}

		public ShaderCompiler<K> enableExtensions(String... extensions)
		{
			return onCompile(($, ctx) -> {
				for (String extension : extensions)
				{
					ctx.enableExtension(extension);
				}
			});
		}

		public ShaderCompiler<K> enableExtensions(Collection<String> extensions)
		{
			return onCompile(($, ctx) -> {
				for (String extension : extensions)
				{
					ctx.enableExtension(extension);
				}
			});
		}

		public ShaderCompiler<K> requireExtensions(Collection<String> extensions)
		{
			return onCompile(($, ctx) -> {
				for (String extension : extensions)
				{
					ctx.requireExtension(extension);
				}
			});
		}

		private boolean shouldCompile(K key, ClrwlShaderSources sources)
		{
			return condition.apply(key, sources);
		}

		private ClrwlGlShader compile(K key, ClrwlShaderCache compiler, ClrwlShaderSources sources)
		{
			long start = System.nanoTime();

			var components = new ArrayList<SourceComponent>();
			for (var fetcher : fetchers)
			{
				components.add(fetcher.apply(key, sources));
			}

			Consumer<ClrwlCompilation> cb = ctx -> compilationCallbacks.accept(key, ctx);
			var name = nameMapper.apply(key);
			var out = compiler.compile(glslVersion, shaderType, name, cb, components);

			long end = System.nanoTime();

			Colorwheel.LOGGER.debug("Compiled {} in {}", name, StringUtil.formatTime(end - start));

			return out;
		}
	}

	public static class ProgramStitcher<K, P extends GlProgram> implements ClrwlCompilationHarness.KeyCompiler<K, P>
	{
		private final Map<ClrwlShaderType, ShaderCompiler<K>> compilers = new EnumMap<>(ClrwlShaderType.class);
		private BiConsumer<K, P> postLink = (k, p) -> {};
		private BiConsumer<K, P> preLink = (k, p) -> {};

		public ClrwlCompilationHarness<K, P> harness(String marker, ClrwlShaderSources sources, BiFunction<K, Integer, P> programInit)
		{
			return new ClrwlCompilationHarness<K, P>(marker, sources, this, programInit);
		}

		public ProgramStitcher<K, P> link(ShaderCompiler<K> compilerBuilder)
		{
			if (compilers.containsKey(compilerBuilder.shaderType))
			{
				throw new IllegalArgumentException("Duplicate shader type: " + compilerBuilder.shaderType);
			}
			compilers.put(compilerBuilder.shaderType, compilerBuilder);
			return this;
		}

		public ProgramStitcher<K, P> postLink(BiConsumer<K, P> postLink)
		{
			this.postLink = postLink;
			return this;
		}

		public ProgramStitcher<K, P> preLink(BiConsumer<K, P> preLink)
		{
			this.preLink = preLink;
			return this;
		}

		@Override
		public P compile(K key, ClrwlShaderSources sources, ClrwlShaderCache shaderCache, ClrwlProgramLinker<K, P> programLinker)
		{
			if (compilers.isEmpty())
			{
				throw new IllegalStateException("No shader compilers were added!");
			}

			long start = System.nanoTime();

			List<ClrwlGlShader> shaders = new ArrayList<>();

			for (ShaderCompiler<K> compiler : compilers.values())
			{
				if (compiler.shouldCompile(key, sources))
				{
					shaders.add(compiler.compile(key, shaderCache, sources));
				}
			}

			var out = programLinker.link(key, shaders, p -> preLink.accept(key, p));

			postLink.accept(key, out);

			long end = System.nanoTime();

			Colorwheel.LOGGER.debug("Linked {} in {}", key, StringUtil.formatTime(end - start));

			return out;
		}
	}
}
