package net.irisshaders.iris.pipeline;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.irisshaders.iris.gl.texture.TextureType;
import net.irisshaders.iris.helpers.Tri;
import net.irisshaders.iris.pipeline.foss_transform.TransformPatcherBridge;
import net.irisshaders.iris.shaderpack.programs.ComputeSource;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shaderpack.texture.TextureStage;
import org.embeddedt.embeddium.impl.gl.shader.ShaderType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Transforms composite-style and compute programs on background threads, so that the renderers compiling them on
 * the render thread only have to wait for whatever hasn't finished yet. Submitting and taking must both happen on
 * the render thread; a program that was never submitted is transformed on the spot when taken.
 */
public class PendingTransforms {
	// Program and compute sources don't override equals, so they are matched by identity.
	private record Key(Object source, TextureStage stage) {}

	private final Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> textureMap;
	private final Map<Key, CompletableFuture<?>> pending = new HashMap<>();

	public PendingTransforms(Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> textureMap) {
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
			pending.computeIfAbsent(new Key(source, stage), k -> CompletableFuture.supplyAsync(() -> transformComposite(source, stage), TransformPatcherBridge.EXECUTOR));
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
				pending.computeIfAbsent(new Key(source, stage), k -> CompletableFuture.supplyAsync(() -> transformCompute(source, stage), TransformPatcherBridge.EXECUTOR));
			}
		}
	}

	@SuppressWarnings("unchecked")
	public Map<ShaderType, String> takeComposite(ProgramSource source, TextureStage stage) {
		CompletableFuture<?> future = pending.remove(new Key(source, stage));
		return future != null ? (Map<ShaderType, String>) await(future) : transformComposite(source, stage);
	}

	public String takeCompute(ComputeSource source, TextureStage stage) {
		CompletableFuture<?> future = pending.remove(new Key(source, stage));
		return future != null ? (String) await(future) : transformCompute(source, stage);
	}

	private Map<ShaderType, String> transformComposite(ProgramSource source, TextureStage stage) {
		return TransformPatcherBridge.patchComposite(source.getName(), source.getSourcesMap(), stage, textureMap);
	}

	private String transformCompute(ComputeSource source, TextureStage stage) {
		return TransformPatcherBridge.patchCompute(source.getName(), source.getSource().orElse(null), stage, textureMap);
	}

	private static Object await(CompletableFuture<?> future) {
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
