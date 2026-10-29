package net.irisshaders.iris.compat.flywheel.indirect;

public final class ClrwlBufferBindings
{
	public static final int TOTAL_BINDING_COUNT = 10;

	public static final int PAGE_FRAME_DESCRIPTOR = 0;
	public static final int BOUNDING_SPHERES = 1;
	public static final int INSTANCE = 2;
	public static final int DRAW_INSTANCE_INDEX = 3;
	public static final int MODEL = 4;
	public static final int DRAW = 5;

	public static final int VISIBILITY = 6;

	public static final int LIGHT_LUT = 7;
	public static final int LIGHT_SECTION = 8;
	public static final int MATRICES = 9;

	// Shader packs bind their own SSBOs starting at 0, so ours go above the pack's highest binding. The constants
	// above are relative to this; programs see it as _CLRWL_SSBO_BASE.
	private static int base = 0;

	private ClrwlBufferBindings()
	{
	}

	public static int base()
	{
		return base;
	}

	public static void setBase(int base)
	{
		ClrwlBufferBindings.base = base;
	}
}
