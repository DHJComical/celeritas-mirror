package net.irisshaders.iris.compat.flywheel.engine;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.instance.InstanceHandle;
import dev.engine_room.flywheel.backend.engine.AbstractInstancer;
import org.jetbrains.annotations.UnknownNullability;

public class ClrwlInstanceHandle<I extends Instance> implements InstanceHandle
{
	@UnknownNullability
	public State<I> state;
	public int index;

	public ClrwlInstanceHandle(@UnknownNullability State<I> state) {
		this.state = state;
	}

	@Override
	public void setChanged() {
		state = state.setChanged(index);
	}

	@Override
	public void setDeleted() {
		state = state.setDeleted(index);
		// invalidate ourselves
		clear();
	}

	@Override
	public void setVisible(boolean visible) {
		state = state.setVisible(this, index, visible);
	}

	@Override
	public boolean isVisible() {
		return state instanceof AbstractInstancer<?>;
	}

	public void clear() {
		index = -1;
	}

	public interface State<I extends Instance>
	{
		State<I> setChanged(int index);

		State<I> setDeleted(int index);

		State<I> setVisible(ClrwlInstanceHandle<I> handle, int index, boolean visible);
	}

	public record Hidden<I extends Instance>(ClrwlAbstractInstancer.Recreate<I> recreate, I instance) implements State<I>
	{
		@Override
		public State<I> setChanged(int index) {
			return this;
		}

		@Override
		public State<I> setDeleted(int index) {
			return this;
		}

		@Override
		public State<I> setVisible(ClrwlInstanceHandle<I> handle, int index, boolean visible) {
			if (!visible) {
				return this;
			}
			var instancer = recreate.recreate();
			return instancer.revealInstance(handle, instance);
		}
	}

	public record Deleted<I extends Instance>() implements State<I>
	{
		private static final Deleted<?> INSTANCE = new Deleted<>();

		@SuppressWarnings("unchecked")
		public static <I extends Instance> Deleted<I> instance() {
			return (Deleted<I>) INSTANCE;
		}

		@Override
		public State<I> setChanged(int index) {
			return this;
		}

		@Override
		public State<I> setDeleted(int index) {
			return this;
		}

		@Override
		public State<I> setVisible(ClrwlInstanceHandle<I> handle, int index, boolean visible) {
			return this;
		}
	}
}
