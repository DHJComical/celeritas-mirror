package net.irisshaders.iris.gl.program;

import com.google.common.collect.ImmutableSet;
import com.mojang.blaze3d.systems.RenderSystem;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.gl.image.ImageHolder;
import net.irisshaders.iris.gl.sampler.GlSampler;
import net.irisshaders.iris.gl.sampler.SamplerHolder;
import net.irisshaders.iris.gl.shader.ProgramCreator;
import net.irisshaders.iris.gl.state.ValueUpdateNotifier;
import net.irisshaders.iris.gl.texture.InternalTextureFormat;
import net.irisshaders.iris.gl.texture.TextureType;
import org.embeddedt.embeddium.impl.gl.shader.GlShader;
import org.embeddedt.embeddium.impl.gl.shader.ShaderType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;

public class ProgramBuilder extends ProgramUniforms.Builder implements SamplerHolder, ImageHolder {
	private static final ShaderType[] GRAPHICS_STAGES = {
		ShaderType.VERTEX, ShaderType.TESS_CTRL, ShaderType.TESS_EVALUATE, ShaderType.GEOM, ShaderType.FRAGMENT
	};

	private final int program;
	private final ProgramSamplers.Builder samplers;
	private final ProgramImages.Builder images;

	private ProgramBuilder(String name, int program, ImmutableSet<Integer> reservedTextureUnits) {
		super(name, program);

		this.program = program;
		this.samplers = ProgramSamplers.builder(program, reservedTextureUnits);
		this.images = ProgramImages.builder(program);
	}

	public static ProgramBuilder begin(String name, @Nullable String vertexSource, @Nullable String geometrySource,
									   @Nullable String fragmentSource, ImmutableSet<Integer> reservedTextureUnits) {
		return finish(submit(name, vertexSource, geometrySource, fragmentSource), reservedTextureUnits);
	}

	public static ProgramBuilder beginCompute(String name, @Nullable String source, ImmutableSet<Integer> reservedTextureUnits) {
		return finish(submitCompute(name, source), reservedTextureUnits);
	}

	/**
	 * Starts compiling and linking a program without waiting for the result. See {@link ProgramCreator#submit}.
	 */
	public static ProgramCreator.Submitted submit(String name, @Nullable String vertexSource, @Nullable String geometrySource,
												  @Nullable String fragmentSource) {
		Map<ShaderType, String> sources = new EnumMap<>(ShaderType.class);
		sources.put(ShaderType.VERTEX, vertexSource);
		sources.put(ShaderType.GEOM, geometrySource);
		sources.put(ShaderType.FRAGMENT, fragmentSource);

		return submit(name, sources, ProgramCreator.DEFAULT_ATTRIBUTE_LOCATIONS);
	}

	/**
	 * Starts compiling and linking a program from its vertex and fragment shaders, plus any tessellation or geometry
	 * shaders present in {@code sources}.
	 */
	public static ProgramCreator.Submitted submit(String name, Map<ShaderType, String> sources, Map<String, Integer> attributeLocations) {
		RenderSystem.assertOnRenderThread();

		List<GlShader.Deferred> shaders = new ArrayList<>(GRAPHICS_STAGES.length);

		for (ShaderType type : GRAPHICS_STAGES) {
			String source = sources.get(type);

			if (source != null || type == ShaderType.VERTEX || type == ShaderType.FRAGMENT) {
				shaders.add(buildShader(type, name + "." + type.fileExtension, source));
			}
		}

		return ProgramCreator.submit(name, shaders, attributeLocations);
	}

	public static ProgramCreator.Submitted submitCompute(String name, @Nullable String source) {
		RenderSystem.assertOnRenderThread();

		if (!IrisRenderSystem.supportsCompute()) {
			throw new IllegalStateException("This PC does not support compute shaders, but it's attempting to be used???");
		}

		return ProgramCreator.submit(name, List.of(buildShader(ShaderType.COMPUTE, name + ".csh", source)), ProgramCreator.DEFAULT_ATTRIBUTE_LOCATIONS);
	}

	public static ProgramBuilder finish(ProgramCreator.Submitted program, ImmutableSet<Integer> reservedTextureUnits) {
		return new ProgramBuilder(program.getName(), program.finish(), reservedTextureUnits);
	}

	private static GlShader.Deferred buildShader(ShaderType shaderType, String name, @Nullable String source) {
		try {
			return GlShader.compileDeferred(shaderType, name, source);
		} catch (RuntimeException e) {
			throw new RuntimeException("Failed to compile " + shaderType + " shader for program " + name, e);
		}
	}

	public void bindAttributeLocation(int index, String name) {
		IrisRenderSystem.bindAttributeLocation(program, index, name);
	}

	public Program build() {
		return new Program(program, super.buildUniforms(), this.samplers.build(), this.images.build());
	}

	public ComputeProgram buildCompute() {
		return new ComputeProgram(program, super.buildUniforms(), this.samplers.build(), this.images.build());
	}

	@Override
	public void addExternalSampler(int textureUnit, String... names) {
		samplers.addExternalSampler(textureUnit, names);
	}

	@Override
	public boolean hasSampler(String name) {
		return samplers.hasSampler(name);
	}

	@Override
	public boolean addDefaultSampler(IntSupplier sampler, String... names) {
		return samplers.addDefaultSampler(sampler, names);
	}

	@Override
	public boolean addDefaultSampler(TextureType type, IntSupplier texture, ValueUpdateNotifier notifier, GlSampler sampler, String... names) {
		return samplers.addDefaultSampler(type, texture, notifier, sampler, names);
	}

	@Override
	public boolean addDynamicSampler(IntSupplier sampler, String... names) {
		return samplers.addDynamicSampler(sampler, names);
	}

	@Override
	public boolean addDynamicSampler(TextureType type, IntSupplier texture, GlSampler sampler, String... names) {
		return samplers.addDynamicSampler(type, texture, sampler, names);
	}

	public boolean addDynamicSampler(IntSupplier sampler, ValueUpdateNotifier notifier, String... names) {
		return samplers.addDynamicSampler(sampler, notifier, names);
	}

	@Override
	public boolean addDynamicSampler(TextureType type, IntSupplier texture, ValueUpdateNotifier notifier, GlSampler sampler, String... names) {
		return samplers.addDynamicSampler(type, texture, notifier, sampler, names);
	}

	@Override
	public boolean hasImage(String name) {
		return images.hasImage(name);
	}

	@Override
	public void addTextureImage(IntSupplier textureID, InternalTextureFormat internalFormat, String name) {
		images.addTextureImage(textureID, internalFormat, name);
	}
}
