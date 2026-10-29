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
import java.util.List;
import java.util.function.IntSupplier;

public class ProgramBuilder extends ProgramUniforms.Builder implements SamplerHolder, ImageHolder {
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
		RenderSystem.assertOnRenderThread();

		List<GlShader.Deferred> shaders = new ArrayList<>(3);
		shaders.add(buildShader(ShaderType.VERTEX, name + ".vsh", vertexSource));

		if (geometrySource != null) {
			shaders.add(buildShader(ShaderType.GEOMETRY, name + ".gsh", geometrySource));
		}

		shaders.add(buildShader(ShaderType.FRAGMENT, name + ".fsh", fragmentSource));

		return ProgramCreator.submit(name, shaders);
	}

	public static ProgramCreator.Submitted submitCompute(String name, @Nullable String source) {
		RenderSystem.assertOnRenderThread();

		if (!IrisRenderSystem.supportsCompute()) {
			throw new IllegalStateException("This PC does not support compute shaders, but it's attempting to be used???");
		}

		return ProgramCreator.submit(name, List.of(buildShader(ShaderType.COMPUTE, name + ".csh", source)));
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
