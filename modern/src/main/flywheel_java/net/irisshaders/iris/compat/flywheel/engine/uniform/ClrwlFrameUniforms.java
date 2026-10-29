package net.irisshaders.iris.compat.flywheel.engine.uniform;

import dev.engine_room.flywheel.api.backend.RenderContext;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.backend.engine.uniform.UniformBuffer;
import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2f;
import org.joml.Vector3f;

public final class ClrwlFrameUniforms extends UniformWriter
{
	private static final int SIZE = 5 * 16 + 2 * 8      // Camera
								  + 4 * 4;     			// Remaining

	public static final UniformBuffer BUFFER = new UniformBuffer(ClrwlUniforms.FRAME_INDEX, SIZE);

	private static final Vector3f CAMERA_POS = new Vector3f();
	private static final Vector3f CAMERA_POS_PREV = new Vector3f();
	private static final Vector3f CAMERA_LOOK = new Vector3f();
	private static final Vector3f CAMERA_LOOK_PREV = new Vector3f();
	private static final Vector2f CAMERA_ROT = new Vector2f();
	private static final Vector2f CAMERA_ROT_PREV = new Vector2f();

	private static boolean firstWrite = true;

	private static int debugMode = DebugMode.OFF.ordinal();

	private ClrwlFrameUniforms() {
	}

	public static void debugMode(DebugMode mode) {
		debugMode = mode.ordinal();
	}

	public static void update(RenderContext context)
	{
		long ptr = BUFFER.ptr();
		setPrev();

		Vec3i renderOrigin = VisualizationManager.getOrThrow(context.level())
				.renderOrigin();
		var camera = context.camera();
		Vec3 cameraPos = camera.getPosition();
		var camX = (float) (cameraPos.x - renderOrigin.getX());
		var camY = (float) (cameraPos.y - renderOrigin.getY());
		var camZ = (float) (cameraPos.z - renderOrigin.getZ());

		CAMERA_POS.set(camX, camY, camZ);
		CAMERA_LOOK.set(camera.getLookVector());
		CAMERA_ROT.set(camera.getXRot(), camera.getYRot());

		if (firstWrite)
		{
			setPrev();
		}

		ptr = writeRenderOrigin(ptr, renderOrigin);

		ptr = writeCamera(ptr);

		ptr = writeCameraIn(ptr, camera);

		ptr = writeInt(ptr, debugMode);

		// OIT noise factor
		ptr = writeFloat(ptr, 0.07f);

		firstWrite = false;
		BUFFER.markDirty();
	}

	private static long writeRenderOrigin(long ptr, Vec3i renderOrigin)
	{
		ptr = writeIVec3(ptr, renderOrigin.getX(), renderOrigin.getY(), renderOrigin.getZ());
		return ptr;
	}

	private static void setPrev()
	{
		CAMERA_POS_PREV.set(CAMERA_POS);
		CAMERA_LOOK_PREV.set(CAMERA_LOOK);
		CAMERA_ROT_PREV.set(CAMERA_ROT);
	}

	private static long writeCamera(long ptr)
	{
		ptr = writeVec3(ptr, CAMERA_POS.x, CAMERA_POS.y, CAMERA_POS.z);
		ptr = writeVec3(ptr, CAMERA_POS_PREV.x, CAMERA_POS_PREV.y, CAMERA_POS_PREV.z);
		ptr = writeVec3(ptr, CAMERA_LOOK.x, CAMERA_LOOK.y, CAMERA_LOOK.z);
		ptr = writeVec3(ptr, CAMERA_LOOK_PREV.x, CAMERA_LOOK_PREV.y, CAMERA_LOOK_PREV.z);
		ptr = writeVec2(ptr, CAMERA_ROT.x, CAMERA_ROT.y);
		ptr = writeVec2(ptr, CAMERA_ROT_PREV.x, CAMERA_ROT_PREV.y);
		return ptr;
	}

	private static long writeCameraIn(long ptr, Camera camera)
	{
		if (!camera.isInitialized())
		{
			ptr = writeInt(ptr, 0);
			ptr = writeInt(ptr, 0);
			return ptr;
		}

		Level level = camera.getEntity().level();
		BlockPos blockPos = camera.getBlockPosition();
		Vec3 cameraPos = camera.getPosition();
		return writeInFluidAndBlock(ptr, level, blockPos, cameraPos);
	}

	public static boolean debugOn()
	{
		return debugMode != DebugMode.OFF.ordinal();
	}
}
