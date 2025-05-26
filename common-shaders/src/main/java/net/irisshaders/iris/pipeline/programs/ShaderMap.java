package net.irisshaders.iris.pipeline.programs;

import org.embeddedt.embeddium.compat.mc.MCShaderInstance;

import java.io.IOException;
import java.util.concurrent.*;

/**
 * A specialized map mapping {@link ShaderKey} to {@link MCShaderInstance}.
 * Avoids much of the complexity / overhead of an EnumMap while ultimately
 * fulfilling the same function.
 */
public class ShaderMap {
	private final MCShaderInstance[] shaders;

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }

	public ShaderMap(ShaderFactory factory, ShaderKey[] ids) throws IOException {
		this.shaders = new MCShaderInstance[ids.length];

        LinkedBlockingQueue<Runnable> renderThreadTasks = new LinkedBlockingQueue<>();
        Executor syncExecutor = renderThreadTasks::add;

        CompletableFuture<MCShaderInstance>[] futures = new CompletableFuture[ids.length];

        for (int i = 0; i < ids.length; i++) {
            futures[i] = factory.create(ids[i], syncExecutor);
            if (futures[i] == null) {
                throw new IllegalArgumentException("Factory for " + ids[i] + " returned null");
            }
        }

        var combinedFuture = CompletableFuture.allOf(futures);

        while (!combinedFuture.isDone()) {
            try {
                var task = renderThreadTasks.take();
                task.run();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

		for (int i = 0; i < ids.length; i++) {
            try {
                this.shaders[i] = futures[i].join();
            } catch (CompletionException e) {
                Throwable trueCause = e;
                while (trueCause instanceof CompletionException) {
                    trueCause = e.getCause();
                }
                sneakyThrow(trueCause);
            }
		}
	}

    public interface ShaderFactory {
        CompletableFuture<MCShaderInstance> create(ShaderKey key, Executor syncExecutor) throws IOException;
    }

	public MCShaderInstance getShader(ShaderKey id) {
		return shaders[id.ordinal()];
	}
}
