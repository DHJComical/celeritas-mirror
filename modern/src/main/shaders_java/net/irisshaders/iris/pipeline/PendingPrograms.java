package net.irisshaders.iris.pipeline;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.irisshaders.iris.gl.program.ProgramBuilder;
import net.irisshaders.iris.gl.shader.ProgramCreator;
import net.irisshaders.iris.gl.texture.TextureType;
import net.irisshaders.iris.helpers.Tri;
import net.irisshaders.iris.pipeline.foss_transform.TransformPatcherBridge;
import net.irisshaders.iris.pipeline.transform.ShaderPrinter;
import net.irisshaders.iris.shaderpack.programs.ComputeSource;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shaderpack.texture.TextureStage;
import org.embeddedt.embeddium.impl.gl.shader.ShaderType;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;

/**
 * Transforms composite-style and compute programs on background threads, then compiles and links all of them before
 * any is checked, so that the driver can work on them together. Renderers take the programs in whatever order they
 * build them and finish each one with {@link ProgramBuilder#finish}. Everything except transformation happens on the
 * render thread; a program that was never submitted is transformed and compiled on the spot when taken.
 */
public class PendingPrograms {
	// Program and compute sources don't override equals, so they are matched by identity.
	private record Key(Object source, TextureStage stage) {}

	private static final class Entry {
		private final String name;
		private final boolean compute;
		private final CompletableFuture<Map<ShaderType, String>> transformed;
		private boolean submitted;
		@Nullable
		private ProgramCreator.Submitted program;
		@Nullable
		private RuntimeException submitFailure;

		private Entry(String name, boolean compute, CompletableFuture<Map<ShaderType, String>> transformed) {
			this.name = name;
			this.compute = compute;
			this.transformed = transformed;
		}

		private void submitProgram(Map<ShaderType, String> sources) {
			submitted = true;

			try {
				program = compute
					? ProgramBuilder.submitCompute(name, sources.get(ShaderType.COMPUTE))
					: ProgramBuilder.submit(name, sources.get(ShaderType.VERTEX), sources.get(ShaderType.GEOMETRY), sources.get(ShaderType.FRAGMENT));
			} catch (RuntimeException e) {
				// Kept until the program is taken, since a program nothing takes shouldn't fail the pipeline.
				submitFailure = e;
			}
		}
	}

	private final Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> textureMap;
	private final Map<Key, Entry> pending = new HashMap<>();

	public PendingPrograms(Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> textureMap) {
		this.textureMap = textureMap;
	}

	public void submit(ProgramSource[] sources, TextureStage stage) {
		for (ProgramSource source : sources) {
			submit(source, stage);
		}
	}

	public void submit(Optional<ProgramSource> source, TextureStage stage) {
		source.ifPresent(s -> submit(s, stage));
	}

	public void submit(ProgramSource source, TextureStage stage) {
		if (source != null && source.isValid()) {
			pending.computeIfAbsent(new Key(source, stage), k -> compositeEntry(source, async(() -> transformComposite(source, stage))));
		}
	}

	public void submitComputes(ComputeSource[][] sources, TextureStage stage) {
		for (ComputeSource[] group : sources) {
			if (group != null) {
				submitComputes(group, stage);
			}
		}
	}

	public void submitComputes(ComputeSource[] sources, TextureStage stage) {
		for (ComputeSource source : sources) {
			if (source != null && source.getSource().isPresent()) {
				pending.computeIfAbsent(new Key(source, stage), k -> computeEntry(source, async(() -> transformCompute(source, stage))));
			}
		}
	}

	/**
	 * Compiles and links every submitted program as its transformation completes, without checking any of them.
	 */
	public void compileAll() {
		LinkedBlockingQueue<Entry> completed = new LinkedBlockingQueue<>();

		for (Entry entry : pending.values()) {
			entry.transformed.whenComplete((result, throwable) -> completed.add(entry));
		}

		for (int i = 0; i < pending.size(); i++) {
			Entry entry;

			try {
				entry = completed.take();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}

			if (!entry.transformed.isCompletedExceptionally()) {
				entry.submitProgram(entry.transformed.join());
			}
		}
	}

	public ProgramCreator.Submitted takeComposite(ProgramSource source, TextureStage stage) {
		return take(new Key(source, stage), () -> compositeEntry(source, CompletableFuture.completedFuture(transformComposite(source, stage))));
	}

	public ProgramCreator.Submitted takeCompute(ComputeSource source, TextureStage stage) {
		return take(new Key(source, stage), () -> computeEntry(source, CompletableFuture.completedFuture(transformCompute(source, stage))));
	}

	/**
	 * Deletes the programs that were compiled but never taken.
	 */
	public void destroyUntaken() {
		for (Entry entry : pending.values()) {
			if (entry.program != null) {
				entry.program.delete();
			}
		}

		pending.clear();
	}

	private ProgramCreator.Submitted take(Key key, Supplier<Entry> createInline) {
		Entry entry = pending.remove(key);

		if (entry == null) {
			entry = createInline.get();
		}

		Map<ShaderType, String> transformed = await(entry.transformed);

		ShaderPrinter.printProgram(entry.name).addSources(transformed).print();

		if (!entry.submitted) {
			entry.submitProgram(transformed);
		}

		if (entry.submitFailure != null) {
			throw entry.submitFailure;
		}

		return entry.program;
	}

	private static Entry compositeEntry(ProgramSource source, CompletableFuture<Map<ShaderType, String>> transformed) {
		return new Entry(source.getName(), false, transformed);
	}

	private static Entry computeEntry(ComputeSource source, CompletableFuture<Map<ShaderType, String>> transformed) {
		return new Entry(source.getName(), true, transformed);
	}

	private static CompletableFuture<Map<ShaderType, String>> async(Supplier<Map<ShaderType, String>> transform) {
		return CompletableFuture.supplyAsync(transform, TransformPatcherBridge.EXECUTOR);
	}

	private Map<ShaderType, String> transformComposite(ProgramSource source, TextureStage stage) {
		return TransformPatcherBridge.patchComposite(source.getName(), source.getSourcesMap(), stage, textureMap);
	}

	private Map<ShaderType, String> transformCompute(ComputeSource source, TextureStage stage) {
		return Collections.singletonMap(ShaderType.COMPUTE, TransformPatcherBridge.patchCompute(source.getName(), source.getSource().orElse(null), stage, textureMap));
	}

	private static <T> T await(CompletableFuture<T> future) {
		try {
			return future.join();
		} catch (CompletionException e) {
			// Rethrow what the transformer threw, so callers' handling of ShaderCompileException etc. still applies.
			if (e.getCause() instanceof RuntimeException cause) {
				throw cause;
			} else if (e.getCause() instanceof Error cause) {
				throw cause;
			}
			throw e;
		}
	}
}
