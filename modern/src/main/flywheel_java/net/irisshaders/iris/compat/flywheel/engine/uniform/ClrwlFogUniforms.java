package net.irisshaders.iris.compat.flywheel.engine.uniform;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.engine_room.flywheel.backend.engine.uniform.UniformBuffer;

public final class ClrwlFogUniforms extends UniformWriter
{
	private static final int SIZE = 4 * 7;
	static final UniformBuffer BUFFER = new UniformBuffer(ClrwlUniforms.FOG_INDEX, SIZE);

	public static void update()
	{
		long ptr = BUFFER.ptr();

		var color = RenderSystem.getShaderFogColor();

		ptr = writeFloat(ptr, color[0]);
		ptr = writeFloat(ptr, color[1]);
		ptr = writeFloat(ptr, color[2]);
		ptr = writeFloat(ptr, color[3]);
		ptr = writeFloat(ptr, RenderSystem.getShaderFogStart());
		ptr = writeFloat(ptr, RenderSystem.getShaderFogEnd());

		var fogShape = RenderSystem.getShaderFogShape();
		// Shouldn't ever be null, but we've seen crashes here.
		ptr = writeInt(ptr, (fogShape == null ? FogShape.SPHERE : fogShape).getIndex());

		BUFFER.markDirty();
	}
}
