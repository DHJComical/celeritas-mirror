package net.irisshaders.iris.compat.flywheel.indirect;

import net.irisshaders.iris.compat.flywheel.engine.embed.EnvironmentStorage;
import dev.engine_room.flywheel.backend.engine.indirect.ResizableStorageArray;
import dev.engine_room.flywheel.backend.engine.indirect.StagingBuffer;
import org.lwjgl.opengl.GL46;
import org.lwjgl.system.MemoryUtil;

public class ClrwlMatrixBuffer
{
	private final ResizableStorageArray matrices;

	public ClrwlMatrixBuffer()
	{
		this.matrices = new ResizableStorageArray(EnvironmentStorage.MATRIX_SIZE_BYTES);
	}

	public void flush(StagingBuffer stagingBuffer, EnvironmentStorage environmentStorage)
	{
		var arena = environmentStorage.arena;
		var capacity = arena.capacity();

		if (capacity == 0)
		{
			return;
		}

		matrices.ensureCapacity(capacity);

		stagingBuffer.enqueueCopy(arena.byteCapacity(), matrices.handle(), 0, ptr ->
		{
			MemoryUtil.memCopy(arena.indexToPointer(0), ptr, arena.byteCapacity());
		});
	}

	public void bind()
	{
		if (matrices.capacity() == 0)
		{
			return;
		}

		GL46.glBindBufferRange(GL46.GL_SHADER_STORAGE_BUFFER, ClrwlBufferBindings.base() + ClrwlBufferBindings.MATRICES, matrices.handle(), 0, matrices.byteCapacity());
	}

	public void delete()
	{
		matrices.delete();
	}
}
