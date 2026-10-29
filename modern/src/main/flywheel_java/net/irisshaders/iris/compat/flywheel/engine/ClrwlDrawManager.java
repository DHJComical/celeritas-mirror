package net.irisshaders.iris.compat.flywheel.engine;

import com.mojang.datafixers.util.Pair;
import net.irisshaders.iris.compat.flywheel.engine.embed.EnvironmentStorage;
import dev.engine_room.flywheel.api.backend.Engine;
import dev.engine_room.flywheel.api.backend.RenderContext;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.task.Plan;
import dev.engine_room.flywheel.backend.FlwBackend;
import dev.engine_room.flywheel.backend.engine.GroupKey;
import dev.engine_room.flywheel.backend.engine.LightStorage;
import dev.engine_room.flywheel.backend.engine.embed.Environment;
import dev.engine_room.flywheel.lib.task.ForEachPlan;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.irisshaders.iris.Iris;
import org.embeddedt.embeddium.impl.gl.debug.GLDebug;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.minecraft.client.resources.model.ModelBakery;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;

public abstract class ClrwlDrawManager<N extends ClrwlAbstractInstancer<?>>
{
	private static final boolean WARN_EMPTY_MODELS = Boolean.getBoolean("flywheel.warnEmptyModels");

	/**
	 * A map of instancer keys to instancers.
	 * <br>
	 * This map is populated as instancers are requested and contains both initialized and uninitialized instancers.
	 */
	protected final Map<ClrwlInstancerKey<?>, N> instancers = new ConcurrentHashMap<>();
	/**
	 * A list of instancers that have not yet been initialized.
	 * <br>
	 * All new instancers land here before having resources allocated in {@link #prepareFrame}.
	 */
	protected final Queue<UninitializedInstancer<N, ?>> initializationQueue = new ConcurrentLinkedQueue<>();

	/**
	 * Function object to pass into computeIfAbsent.
	 * <p>Create once and cache to avoid allocating every time.
	 */
	protected final Function<ClrwlInstancerKey<?>, N> createAndDeferInit = this::createAndDeferInit;

	public <I extends Instance> ClrwlAbstractInstancer<I> getInstancer(ClrwlInstanceVisual visual, Environment environment, InstanceType<I> type, Model model, int bias)
	{
		return getInstancer(new ClrwlInstancerKey<>(visual, environment, type, model, bias));
	}

	@SuppressWarnings("unchecked")
	public <I extends Instance> ClrwlAbstractInstancer<I> getInstancer(ClrwlInstancerKey<I> key)
	{
		return (ClrwlAbstractInstancer<I>) instancers.computeIfAbsent(key, createAndDeferInit);
	}

	public Plan<RenderContext> createFramePlan()
	{
		// Go wide on instancers to process deletions in parallel.
		return ForEachPlan.of(() -> new ArrayList<>(instancers.values()), ClrwlAbstractInstancer::parallelUpdate);
	}

	public void prepareFrame(LightStorage lightStorage, EnvironmentStorage environmentStorage)
	{
		// Thread safety: flush is called from the render thread after all visual updates have been made,
		// so there are no:tm: threads we could be racing with.
		for (var init : initializationQueue)
		{
			var instancer = init.instancer();

			if (instancer.instanceCount() > 0)
			{
				initialize(init.key(), instancer);
			}
			else
			{
				instancers.remove(init.key());
			}
		}

		initializationQueue.clear();
	}

	public abstract void preparePass(IrisRenderingPipeline pipeline, boolean isShadow);
	public abstract void renderSolid(IrisRenderingPipeline pipeline, boolean isShadow);
	public abstract void renderTranslucent(IrisRenderingPipeline pipeline, boolean isShadow);

	public abstract void renderCrumbling(IrisRenderingPipeline pipeline, List<Engine.CrumblingBlock> crumblingBlocks);


	public void onRenderOriginChanged()
	{
		instancers.values()
				.forEach(ClrwlAbstractInstancer::clear);
	}

	protected abstract <I extends Instance> N create(ClrwlInstancerKey<I> type);

	protected abstract <I extends Instance> void initialize(ClrwlInstancerKey<I> key, N instancer);

	private N createAndDeferInit(ClrwlInstancerKey<?> key)
	{
		var out = create(key);

		// Only queue the instancer for initialization if it has anything to render.
		if (checkAndWarnEmptyModel(key.model())) {
			// Thread safety: this method is called atomically from within computeIfAbsent,
			// so you'd think we don't need extra synchronization to protect the queue, but
			// somehow threads can race here and wind up never initializing an instancer.
			initializationQueue.add(new UninitializedInstancer<>(key, out));
		}
		return out;
	}

	private static boolean checkAndWarnEmptyModel(Model model)
	{
		if (!model.meshes().isEmpty()) {
			return true;
		}

		if (WARN_EMPTY_MODELS) {
			StringBuilder builder = new StringBuilder();
			builder.append("Creating an instancer for a model with no meshes! Stack trace:");

			StackWalker.getInstance()
					.forEach(f -> builder.append("\n\t")
							.append(f.toString()));

			FlwBackend.LOGGER.warn(builder.toString());
		}

		return false;
	}

	@FunctionalInterface
	protected interface State2Instancer<I extends ClrwlAbstractInstancer<?>>
	{
		// I tried using a plain Function<State<?>, I> here, but it exploded with type errors.
		@Nullable I apply(ClrwlInstanceHandle.State<?> state);
	}

	protected static <I extends ClrwlAbstractInstancer<?>> Map<GroupKey<?>, Int2ObjectMap<List<Pair<I, ClrwlInstanceHandle<?>>>>> doCrumblingSort(List<Engine.CrumblingBlock> crumblingBlocks, State2Instancer<I> cast)
	{
		Map<GroupKey<?>, Int2ObjectMap<List<Pair<I, ClrwlInstanceHandle<?>>>>> byType = new HashMap<>();
		for (Engine.CrumblingBlock block : crumblingBlocks)
		{
			int progress = block.progress();

			if (progress < 0 || progress >= ModelBakery.DESTROY_TYPES.size())
			{
				continue;
			}

			for (Instance instance : block.instances())
			{
				// Filter out instances that weren't created by this engine.
				// If all is well, we probably shouldn't take the `continue`
				// branches but better to do checked casts.
				if (!(instance.handle() instanceof ClrwlInstanceHandle<?> impl))
				{
					continue;
				}

				var instancer = cast.apply(impl.state);

				if (instancer == null)
				{
					continue;
				}

				byType.computeIfAbsent(new GroupKey<>(instancer.type, instancer.environment), $ -> new Int2ObjectArrayMap<>())
						.computeIfAbsent(progress, $ -> new ArrayList<>())
						.add(Pair.of(instancer, impl));
			}
		}
		return byType;
	}

	public void delete()
	{
		instancers.clear();
		initializationQueue.clear();
	}

	public abstract void triggerFallback();

	protected String getShaderPackName()
	{
		return Iris.getCurrentPackName();
	}

	protected ClrwlRenderingPhase currentRenderPhase = ClrwlRenderingPhase.SOLID;

	protected void setPhase(ClrwlRenderingPhase phase, boolean shadow)
	{
		var name = "Clrwl " + (shadow ? "Shadow " : "") + StringUtils.capitalize(phase.name().toLowerCase(Locale.ROOT).replace("_", " "));

		GLDebug.popGroup();
		GLDebug.pushGroup(phase.getValue(), name);

		currentRenderPhase = phase;
	}

	protected record UninitializedInstancer<N, I extends Instance>(ClrwlInstancerKey<I> key, N instancer) {
	}
}
