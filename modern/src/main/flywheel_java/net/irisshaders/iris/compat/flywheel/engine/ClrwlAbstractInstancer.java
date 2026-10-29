package net.irisshaders.iris.compat.flywheel.engine;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.api.instance.Instancer;
import dev.engine_room.flywheel.backend.engine.embed.Environment;

public abstract class ClrwlAbstractInstancer<I extends Instance> implements Instancer<I>
{
	public final ClrwlInstanceVisual visual;
	public final InstanceType<I> type;
	public final Environment environment;
	public final Recreate<I> recreate;

	protected ClrwlAbstractInstancer(ClrwlInstancerKey<I> key, Recreate<I> recreate)
	{
		this.visual = key.visual();
		this.type = key.type();
		this.environment = key.environment();
		this.recreate = recreate;
	}

	public abstract ClrwlInstanceHandle.State<I> revealInstance(ClrwlInstanceHandle<I> handle, I instance);

	public abstract int instanceCount();

	public abstract void parallelUpdate();

	public abstract void delete();

	public abstract void clear();

	@Override
	public String toString() {
		return "ClrwlAbstractInstancer[" + instanceCount() + ']';
	}

	public record Recreate<I extends Instance>(ClrwlInstancerKey<I> key, ClrwlDrawManager<?> drawManager)
	{
		public ClrwlAbstractInstancer<I> recreate()
		{
			return drawManager.getInstancer(key);
		}
	}
}
