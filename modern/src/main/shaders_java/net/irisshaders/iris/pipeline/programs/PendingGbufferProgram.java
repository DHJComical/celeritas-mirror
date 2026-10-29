package net.irisshaders.iris.pipeline.programs;

import com.google.common.collect.ImmutableSet;
import net.irisshaders.iris.gl.program.ProgramBuilder;
import net.irisshaders.iris.gl.shader.ProgramCreator;
import net.irisshaders.iris.pipeline.PendingPrograms;
import net.irisshaders.iris.pipeline.transform.ShaderPrinter;
import org.embeddedt.embeddium.impl.gl.shader.ShaderType;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * A gbuffer program whose sources may still be transforming. {@link #submit()} starts compiling it once they are
 * ready, and {@link #finish()} waits for the compile and builds the {@link GbufferProgram}.
 */
public final class PendingGbufferProgram {
	public interface Builder {
		/**
		 * Adds the program's uniforms and samplers to the builder, and creates the program from it.
		 */
		GbufferProgram build(ProgramBuilder builder, Map<ShaderType, String> sources);
	}

	private final String name;
	private final Map<String, Integer> attributeLocations;
	private final ImmutableSet<Integer> reservedTextureUnits;
	private final CompletableFuture<Map<ShaderType, String>> sources;
	private final Builder builder;
	@Nullable
	private ProgramCreator.Submitted submitted;

	PendingGbufferProgram(String name, Map<String, Integer> attributeLocations, ImmutableSet<Integer> reservedTextureUnits,
						  CompletableFuture<Map<ShaderType, String>> sources, Builder builder) {
		this.name = name;
		this.attributeLocations = attributeLocations;
		this.reservedTextureUnits = reservedTextureUnits;
		this.sources = sources;
		this.builder = builder;
	}

	public void whenTransformed(Runnable action) {
		sources.whenComplete((result, throwable) -> action.run());
	}

	/**
	 * Starts compiling the program once its sources are transformed. If transforming failed, {@link #finish()} throws
	 * the failure instead.
	 */
	public void submit() {
		if (submitted == null && !sources.isCompletedExceptionally()) {
			Map<ShaderType, String> transformed = sources.join();
			ShaderPrinter.printProgram(name).addSources(transformed).print();
			submitted = ProgramBuilder.submit(name, transformed, attributeLocations);
		}
	}

	public GbufferProgram finish() {
		Map<ShaderType, String> transformed = PendingPrograms.await(sources);
		submit();

		ProgramCreator.Submitted program = submitted;
		submitted = null;

		return builder.build(ProgramBuilder.finish(program, reservedTextureUnits), transformed);
	}

	/**
	 * Deletes the program if it was submitted and not finished.
	 */
	public void delete() {
		if (submitted != null) {
			submitted.delete();
			submitted = null;
		}
	}
}
