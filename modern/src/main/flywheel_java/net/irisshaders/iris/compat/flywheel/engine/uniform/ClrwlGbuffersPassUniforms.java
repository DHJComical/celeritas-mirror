package net.irisshaders.iris.compat.flywheel.engine.uniform;

import net.irisshaders.iris.compat.flywheel.indirect.ClrwlDepthPyramid;
import dev.engine_room.flywheel.api.backend.RenderContext;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.backend.engine.uniform.UniformBuffer;
import dev.engine_room.flywheel.backend.mixin.LevelRendererAccessor;
import net.irisshaders.iris.shaderpack.ShaderPack;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.irisshaders.iris.shaderpack.properties.PackDirectives;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import org.joml.Math;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

public final class ClrwlGbuffersPassUniforms extends UniformWriter
{
	private static final int SIZE = 96 	        		// Frustum
								  + 32 	        		// Cull
								  + 64 * 9      		// View + Projection
								  + 48 		    		// Normal
								  + 6 * 8;     			// Remaining

	public static final UniformBuffer BUFFER = new UniformBuffer(ClrwlUniforms.PASS_INDEX, SIZE);

	private static final Matrix4f VIEW = new Matrix4f();
	private static final Matrix4f VIEW_INVERSE = new Matrix4f();
	private static final Matrix4f VIEW_PREV = new Matrix4f();
	private static final Matrix4f PROJECTION = new Matrix4f();
	private static final Matrix4f PROJECTION_INVERSE = new Matrix4f();
	private static final Matrix4f PROJECTION_PREV = new Matrix4f();
	private static final Matrix4f VIEW_PROJECTION = new Matrix4f();
	private static final Matrix4f VIEW_PROJECTION_INVERSE = new Matrix4f();
	private static final Matrix4f VIEW_PROJECTION_PREV = new Matrix4f();

	private static final Matrix3f NORMAL = new Matrix3f();

	private static boolean firstWrite = true;

	private static boolean frustumPaused = false;
	private static boolean frustumCapture = false;

	private ClrwlGbuffersPassUniforms() {
	}

	public static void captureFrustum()
	{
		frustumPaused = true;
		frustumCapture = true;
	}

	public static void unpauseFrustum()
	{
		frustumPaused = false;
	}

	public static void update(RenderContext context, ShaderPack pack, NamespacedId dimension)
	{
		PackDirectives directives = pack.getProgramSet(dimension).getPackDirectives();

		long ptr = BUFFER.ptr();
		setPrev();

		Vec3i renderOrigin = VisualizationManager.getOrThrow(context.level())
				.renderOrigin();
		var camera = context.camera();
		Vec3 cameraPos = camera.getPosition();
		var camX = (float) (cameraPos.x - renderOrigin.getX());
		var camY = (float) (cameraPos.y - renderOrigin.getY());
		var camZ = (float) (cameraPos.z - renderOrigin.getZ());

		VIEW.set(context./*? if >=1.21 {*/ /*modelView() *//*?} else {*/ stack().last().pose() /*?}*/);
		VIEW.translate(-camX, -camY, -camZ);
		PROJECTION.set(context.projection());
		VIEW_PROJECTION.set(context.viewProjection());
		VIEW_PROJECTION.translate(-camX, -camY, -camZ);
		
		Matrix4f normal = new Matrix4f(context./*? if >=1.21 {*/ /*modelView() *//*?} else {*/ stack().last().pose() /*?}*/)
				.translate(-camX, -camY, -camZ)
				.invert()
				.transpose();
		normal.get3x3(NORMAL);

		if (firstWrite)
		{
			setPrev();
		}

		if (firstWrite || !frustumPaused || frustumCapture)
		{
			writePackedFrustumPlanes(ptr, VIEW_PROJECTION);
			frustumCapture = false;
		}

		ptr += 96;

		ptr = writeCullData(ptr);

		ptr = writeMatrices(ptr);

		var window = Minecraft.getInstance()
				.getWindow();
		ptr = writeVec2(ptr, window.getWidth(), window.getHeight());
		ptr = writeFloat(ptr, (float) window.getWidth() / (float) window.getHeight());
		// default line width: net.minecraft.client.renderer.RenderStateShard.LineStateShard
		ptr = writeFloat(ptr, Math.max(2.5F, (float) window.getWidth() / 1920.0F * 2.5F));
		ptr = writeFloat(ptr, Minecraft.getInstance().gameRenderer.getDepthFar());

		ptr = writeTime(ptr, context);

		firstWrite = false;
		BUFFER.markDirty();
	}

	private static void setPrev()
	{
		VIEW_PREV.set(VIEW);
		PROJECTION_PREV.set(PROJECTION);
		VIEW_PROJECTION_PREV.set(VIEW_PROJECTION);
	}

	private static long writeMatrices(long ptr)
	{
		ptr = writeMat4(ptr, VIEW);
		ptr = writeMat4(ptr, VIEW.invert(VIEW_INVERSE));
		ptr = writeMat4(ptr, VIEW_PREV);
		ptr = writeMat4(ptr, PROJECTION);
		ptr = writeMat4(ptr, PROJECTION.invert(PROJECTION_INVERSE));
		ptr = writeMat4(ptr, PROJECTION_PREV);
		ptr = writeMat4(ptr, VIEW_PROJECTION);
		ptr = writeMat4(ptr, VIEW_PROJECTION.invert(VIEW_PROJECTION_INVERSE));
		ptr = writeMat4(ptr, VIEW_PROJECTION_PREV);
		ptr = writeMat3(ptr, NORMAL);
		return ptr;
	}

	private static long writeTime(long ptr, RenderContext context)
	{
		int ticks = ((LevelRendererAccessor) context.renderer()).flywheel$getTicks();
		float partialTick = context.partialTick();
		float renderTicks = ticks + partialTick;
		float renderSeconds = renderTicks / 20f;
		float systemSeconds = Util.getMillis() / 1000f;
		int systemMillis = (int) (Util.getMillis() % Integer.MAX_VALUE);

		ptr = writeInt(ptr, ticks);
		ptr = writeFloat(ptr, partialTick);
		ptr = writeFloat(ptr, renderTicks);
		ptr = writeFloat(ptr, renderSeconds);
		ptr = writeFloat(ptr, systemSeconds);
		ptr = writeInt(ptr, systemMillis);
		return ptr;
	}

	private static long writeCullData(long ptr)
	{
		var mc = Minecraft.getInstance();
		var mainRenderTarget = mc.getMainRenderTarget();

		int pyramidWidth = ClrwlDepthPyramid.mip0Size(mainRenderTarget.width);
		int pyramidHeight = ClrwlDepthPyramid.mip0Size(mainRenderTarget.height);
		int pyramidDepth = ClrwlDepthPyramid.getImageMipLevels(pyramidWidth, pyramidHeight);

		ptr = writeFloat(ptr, GameRenderer.PROJECTION_Z_NEAR); // zNear
		ptr = writeFloat(ptr, mc.gameRenderer.getDepthFar()); // zFar
		ptr = writeFloat(ptr, PROJECTION.m00()); // P00
		ptr = writeFloat(ptr, PROJECTION.m11()); // P11
		ptr = writeFloat(ptr, pyramidWidth); // pyramidWidth
		ptr = writeFloat(ptr, pyramidHeight); // pyramidHeight
		ptr = writeInt(ptr, pyramidDepth - 1); // pyramidLevels
		ptr = writeInt(ptr, 0); // orthographicProjection

		return ptr;
	}

	/**
	 * Writes the frustum planes of the given projection matrix to the given buffer.<p>
	 * Uses a different format that is friendly towards an optimized instruction-parallel
	 * implementation of sphere-frustum intersection.<p>
	 * The format is as follows:<p>
	 * {@code vec4(nxX, pxX, nyX, pyX)}<br>
	 * {@code vec4(nxY, pxY, nyY, pyY)}<br>
	 * {@code vec4(nxZ, pxZ, nyZ, pyZ)}<br>
	 * {@code vec4(nxW, pxW, nyW, pyW)}<br>
	 * {@code vec2(nzX, pzX)}<br>
	 * {@code vec2(nzY, pzY)}<br>
	 * {@code vec2(nzZ, pzZ)}<br>
	 * {@code vec2(nzW, pzW)}<br>
	 * <p>
	 * Writes 96 bytes to the buffer.
	 *
	 * @param ptr The buffer to write the planes to.
	 * @param m   The projection matrix to compute the frustum planes for.
	 */
	private static void writePackedFrustumPlanes(long ptr, Matrix4f m) {
		float nxX, nxY, nxZ, nxW;
		float pxX, pxY, pxZ, pxW;
		float nyX, nyY, nyZ, nyW;
		float pyX, pyY, pyZ, pyW;
		float nzX, nzY, nzZ, nzW;
		float pzX, pzY, pzZ, pzW;

		float invl;
		nxX = m.m03() + m.m00();
		nxY = m.m13() + m.m10();
		nxZ = m.m23() + m.m20();
		nxW = m.m33() + m.m30();
		invl = Math.invsqrt(nxX * nxX + nxY * nxY + nxZ * nxZ);
		nxX *= invl;
		nxY *= invl;
		nxZ *= invl;
		nxW *= invl;

		pxX = m.m03() - m.m00();
		pxY = m.m13() - m.m10();
		pxZ = m.m23() - m.m20();
		pxW = m.m33() - m.m30();
		invl = Math.invsqrt(pxX * pxX + pxY * pxY + pxZ * pxZ);
		pxX *= invl;
		pxY *= invl;
		pxZ *= invl;
		pxW *= invl;

		nyX = m.m03() + m.m01();
		nyY = m.m13() + m.m11();
		nyZ = m.m23() + m.m21();
		nyW = m.m33() + m.m31();
		invl = Math.invsqrt(nyX * nyX + nyY * nyY + nyZ * nyZ);
		nyX *= invl;
		nyY *= invl;
		nyZ *= invl;
		nyW *= invl;

		pyX = m.m03() - m.m01();
		pyY = m.m13() - m.m11();
		pyZ = m.m23() - m.m21();
		pyW = m.m33() - m.m31();
		invl = Math.invsqrt(pyX * pyX + pyY * pyY + pyZ * pyZ);
		pyX *= invl;
		pyY *= invl;
		pyZ *= invl;
		pyW *= invl;

		nzX = m.m03() + m.m02();
		nzY = m.m13() + m.m12();
		nzZ = m.m23() + m.m22();
		nzW = m.m33() + m.m32();
		invl = Math.invsqrt(nzX * nzX + nzY * nzY + nzZ * nzZ);
		nzX *= invl;
		nzY *= invl;
		nzZ *= invl;
		nzW *= invl;

		pzX = m.m03() - m.m02();
		pzY = m.m13() - m.m12();
		pzZ = m.m23() - m.m22();
		pzW = m.m33() - m.m32();
		invl = Math.invsqrt(pzX * pzX + pzY * pzY + pzZ * pzZ);
		pzX *= invl;
		pzY *= invl;
		pzZ *= invl;
		pzW *= invl;

		MemoryUtil.memPutFloat(ptr, nxX);
		MemoryUtil.memPutFloat(ptr + 4, pxX);
		MemoryUtil.memPutFloat(ptr + 8, nyX);
		MemoryUtil.memPutFloat(ptr + 12, pyX);
		MemoryUtil.memPutFloat(ptr + 16, nxY);
		MemoryUtil.memPutFloat(ptr + 20, pxY);
		MemoryUtil.memPutFloat(ptr + 24, nyY);
		MemoryUtil.memPutFloat(ptr + 28, pyY);
		MemoryUtil.memPutFloat(ptr + 32, nxZ);
		MemoryUtil.memPutFloat(ptr + 36, pxZ);
		MemoryUtil.memPutFloat(ptr + 40, nyZ);
		MemoryUtil.memPutFloat(ptr + 44, pyZ);
		MemoryUtil.memPutFloat(ptr + 48, nxW);
		MemoryUtil.memPutFloat(ptr + 52, pxW);
		MemoryUtil.memPutFloat(ptr + 56, nyW);
		MemoryUtil.memPutFloat(ptr + 60, pyW);
		MemoryUtil.memPutFloat(ptr + 64, nzX);
		MemoryUtil.memPutFloat(ptr + 68, pzX);
		MemoryUtil.memPutFloat(ptr + 72, nzY);
		MemoryUtil.memPutFloat(ptr + 76, pzY);
		MemoryUtil.memPutFloat(ptr + 80, nzZ);
		MemoryUtil.memPutFloat(ptr + 84, pzZ);
		MemoryUtil.memPutFloat(ptr + 88, nzW);
		MemoryUtil.memPutFloat(ptr + 92, pzW);
	}
}
