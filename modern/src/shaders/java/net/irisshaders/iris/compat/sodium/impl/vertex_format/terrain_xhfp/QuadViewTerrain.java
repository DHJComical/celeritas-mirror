package net.irisshaders.iris.compat.sodium.impl.vertex_format.terrain_xhfp;

import net.irisshaders.iris.vertices.views.QuadView;
import org.lwjgl.system.MemoryUtil;

public abstract class QuadViewTerrain implements QuadView {
	long writePointer;
	int stride;

	@Override
	public float x(int index) {
		return getFloat(writePointer - (long) stride * (3 - index));
	}

	@Override
	public float y(int index) {
		return getFloat(writePointer + 4 - (long) stride * (3 - index));
	}

	@Override
	public float z(int index) {
		return getFloat(writePointer + 8 - (long) stride * (3 - index));
	}

	@Override
	public float u(int index) {
		return getFloat(writePointer + 16 - (long) stride * (3 - index));
	}

	@Override
	public float v(int index) {
		return getFloat(writePointer + 20 - (long) stride * (3 - index));
	}

	abstract float getFloat(long writePointer);

	public static class QuadViewTerrainUnsafe extends QuadViewTerrain {
		public void setup(long writePointer, int stride) {
			this.writePointer = writePointer;
			this.stride = stride;
		}

		@Override
		float getFloat(long writePointer) {
			return MemoryUtil.memGetFloat(writePointer);
		}
	}
}
