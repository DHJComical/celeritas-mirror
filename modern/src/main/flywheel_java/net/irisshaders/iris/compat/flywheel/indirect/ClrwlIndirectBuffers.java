package net.irisshaders.iris.compat.flywheel.indirect;

import dev.engine_room.flywheel.backend.engine.indirect.ObjectStorage;
import dev.engine_room.flywheel.backend.engine.indirect.ResizableStorageArray;
import dev.engine_room.flywheel.backend.engine.indirect.StagingBuffer;
import dev.engine_room.flywheel.backend.gl.buffer.GlBufferType;
import dev.engine_room.flywheel.lib.memory.MemoryBlock;
import org.lwjgl.opengl.GL46;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Pointer;

import static org.lwjgl.opengl.GL30.glBindBufferBase;
import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER;
import static org.lwjgl.opengl.GL44.nglBindBuffersRange;

public class ClrwlIndirectBuffers
{
    // Number of vbos created.
    public static final int BUFFER_COUNT = 6;

    public static final long INT_SIZE = Integer.BYTES;
    public static final long PTR_SIZE = Pointer.POINTER_SIZE;
    public static final long SPHERE_SIZE = Float.BYTES * 4L;

    public static final long MODEL_STRIDE = 28 + 12;

    // Byte size of a draw command, plus our added mesh data.
    public static final long DRAW_COMMAND_STRIDE = 36 + 12;
    public static final long DRAW_COMMAND_OFFSET = 0;

    // Offsets to the 3 segments
    private static final long HANDLE_OFFSET = 0;
    private static final long OFFSET_OFFSET = BUFFER_COUNT * INT_SIZE;
    private static final long SIZE_OFFSET = OFFSET_OFFSET + BUFFER_COUNT * PTR_SIZE;
    // Total size of the buffer.
    private static final long BUFFERS_SIZE_BYTES = SIZE_OFFSET + BUFFER_COUNT * PTR_SIZE;

    // Offsets to the vbos
    private static final long PAGE_FRAME_DESCRIPTOR_HANDLE_OFFSET = HANDLE_OFFSET + ClrwlBufferBindings.PAGE_FRAME_DESCRIPTOR * INT_SIZE;
    private static final long BOUNDING_SPHERE_HANDLE_OFFSET = HANDLE_OFFSET + ClrwlBufferBindings.BOUNDING_SPHERES * INT_SIZE;
    private static final long INSTANCE_HANDLE_OFFSET = HANDLE_OFFSET + ClrwlBufferBindings.INSTANCE * INT_SIZE;
    private static final long DRAW_INSTANCE_INDEX_HANDLE_OFFSET = HANDLE_OFFSET + ClrwlBufferBindings.DRAW_INSTANCE_INDEX * INT_SIZE;
    private static final long MODEL_HANDLE_OFFSET = HANDLE_OFFSET + ClrwlBufferBindings.MODEL * INT_SIZE;
    private static final long DRAW_HANDLE_OFFSET = HANDLE_OFFSET + ClrwlBufferBindings.DRAW * INT_SIZE;

    // Offsets to the sizes
    private static final long PAGE_FRAME_DESCRIPTOR_SIZE_OFFSET = SIZE_OFFSET + ClrwlBufferBindings.PAGE_FRAME_DESCRIPTOR * PTR_SIZE;
    private static final long BOUNDING_SPHERE_SIZE_OFFSET = SIZE_OFFSET + ClrwlBufferBindings.BOUNDING_SPHERES * PTR_SIZE;
    private static final long INSTANCE_SIZE_OFFSET = SIZE_OFFSET + ClrwlBufferBindings.INSTANCE * PTR_SIZE;
    private static final long DRAW_INSTANCE_INDEX_SIZE_OFFSET = SIZE_OFFSET + ClrwlBufferBindings.DRAW_INSTANCE_INDEX * PTR_SIZE;
    private static final long MODEL_SIZE_OFFSET = SIZE_OFFSET + ClrwlBufferBindings.MODEL * PTR_SIZE;
    private static final long DRAW_SIZE_OFFSET = SIZE_OFFSET + ClrwlBufferBindings.DRAW * PTR_SIZE;


    private static final float INSTANCE_GROWTH_FACTOR = 1.25f;
    private static final float MODEL_GROWTH_FACTOR = 2f;
    private static final float DRAW_GROWTH_FACTOR = 2f;

    /**
     * A small block of memory divided into 3 contiguous segments:
     * <br>
     * {@code buffers}: an array of {@link net.irisshaders.iris.compat.flywheel.indirect.ClrwlIndirectBuffers#INT_SIZE} buffer handles.
     * <br>
     * {@code offsets}: an array of {@link net.irisshaders.iris.compat.flywheel.indirect.ClrwlIndirectBuffers#PTR_SIZE} offsets into the buffers, currently just zeroed.
     * <br>
     * {@code sizes}: an array of {@link net.irisshaders.iris.compat.flywheel.indirect.ClrwlIndirectBuffers#PTR_SIZE} byte lengths of the buffers.
     * <br>
     * Each segment stores {@link net.irisshaders.iris.compat.flywheel.indirect.ClrwlIndirectBuffers#BUFFER_COUNT} elements,
     * one for the instance buffer, target buffer, model index buffer, model buffer, and draw buffer.
     */
    private final MemoryBlock multiBindBlock;

    public final ObjectStorage objectStorage;
    public final ResizableStorageArray boundingSpheres;
    public final ResizableStorageArray drawInstanceIndex;
    public final ResizableStorageArray model;
    public final ResizableStorageArray draw;

    public ClrwlIndirectBuffers(long instanceStride)
    {
        this.multiBindBlock = MemoryBlock.calloc(BUFFERS_SIZE_BYTES, 1);

        objectStorage = new ObjectStorage(instanceStride);
        boundingSpheres = new ResizableStorageArray(SPHERE_SIZE, INSTANCE_GROWTH_FACTOR);
        drawInstanceIndex = new ResizableStorageArray(INT_SIZE, INSTANCE_GROWTH_FACTOR);
        model = new ResizableStorageArray(MODEL_STRIDE, MODEL_GROWTH_FACTOR);
        draw = new ResizableStorageArray(DRAW_COMMAND_STRIDE, DRAW_GROWTH_FACTOR);
    }

    public void updateCounts(int instanceCount, int modelCount, int drawCount)
    {
        final long ptr = multiBindBlock.ptr();

        boundingSpheres.ensureCapacity(getAllocatedInstanceCount());
        drawInstanceIndex.ensureCapacity(instanceCount);
        model.ensureCapacity(modelCount);
        draw.ensureCapacity(drawCount);

        MemoryUtil.memPutInt(ptr + PAGE_FRAME_DESCRIPTOR_HANDLE_OFFSET, objectStorage.frameDescriptorBuffer.handle());
        MemoryUtil.memPutInt(ptr + BOUNDING_SPHERE_HANDLE_OFFSET, boundingSpheres.handle());
        MemoryUtil.memPutInt(ptr + INSTANCE_HANDLE_OFFSET, objectStorage.objectBuffer.handle());
        MemoryUtil.memPutInt(ptr + DRAW_INSTANCE_INDEX_HANDLE_OFFSET, drawInstanceIndex.handle());
        MemoryUtil.memPutInt(ptr + MODEL_HANDLE_OFFSET, model.handle());
        MemoryUtil.memPutInt(ptr + DRAW_HANDLE_OFFSET, draw.handle());

        MemoryUtil.memPutAddress(ptr + PAGE_FRAME_DESCRIPTOR_SIZE_OFFSET, objectStorage.frameDescriptorBuffer.capacity());
        MemoryUtil.memPutAddress(ptr + BOUNDING_SPHERE_SIZE_OFFSET, SPHERE_SIZE * boundingSpheres.capacity());
        MemoryUtil.memPutAddress(ptr + INSTANCE_SIZE_OFFSET, objectStorage.objectBuffer.capacity());
        MemoryUtil.memPutAddress(ptr + DRAW_INSTANCE_INDEX_SIZE_OFFSET, INT_SIZE * instanceCount);
        MemoryUtil.memPutAddress(ptr + MODEL_SIZE_OFFSET, MODEL_STRIDE * modelCount);
        MemoryUtil.memPutAddress(ptr + DRAW_SIZE_OFFSET, DRAW_COMMAND_STRIDE * drawCount);
    }

    private long getAllocatedPageCount()
    {
        return objectStorage.frameDescriptorBuffer.capacity() / 2L / 4L; // 2 * 4 bytes per page
    }

    private long getAllocatedInstanceCount()
    {
        return getAllocatedPageCount() * 32L; // 1 page = 32 instances
    }

    public void bindForTransform()
    {
        multiBind(0, 5);
    }

    public void bindForCull()
    {
        multiBind(0, 5);
    }

    public void bindForApply()
    {
        multiBind(4, 2);
    }

    public void bindForModelReset()
    {
        multiBind(4, 1);
    }

    public void bindForDraw()
    {
        multiBind(2, 4);
        GlBufferType.DRAW_INDIRECT_BUFFER.bind(draw.handle());
    }

    public void bindForCrumbling()
    {
        // All we need is the instance buffer. Crumbling uses its own draw buffer.
        multiBind(ClrwlBufferBindings.INSTANCE, 1);
    }

    private void multiBind(int base, int count)
    {
        final long ptr = multiBindBlock.ptr();
        nglBindBuffersRange(GL_SHADER_STORAGE_BUFFER, ClrwlBufferBindings.base() + base, count, ptr + base * INT_SIZE, ptr + OFFSET_OFFSET + base * PTR_SIZE, ptr + SIZE_OFFSET + base * PTR_SIZE);
    }

    public void delete()
    {
        multiBindBlock.free();

        objectStorage.delete();
        boundingSpheres.delete();
        drawInstanceIndex.delete();
        model.delete();
        draw.delete();
    }

    public PipelineBuffers makePipelineBuffers()
    {
        return new PipelineBuffers();
    }

    public DrawSnapshot makeDrawSnapshot()
    {
        return new DrawSnapshot();
    }

    public class PipelineBuffers
    {
        public final ResizableStorageArray lastFrameVisibility;
        public final ResizableStorageArray shadowLastFrameVisibility;

        public PipelineBuffers()
        {
            lastFrameVisibility = new ResizableStorageArray(INT_SIZE, INSTANCE_GROWTH_FACTOR);
            shadowLastFrameVisibility = new ResizableStorageArray(INT_SIZE, INSTANCE_GROWTH_FACTOR);
        }

        public void updateCounts()
        {
            lastFrameVisibility.ensureCapacity(ClrwlIndirectBuffers.this.getAllocatedPageCount());
            shadowLastFrameVisibility.ensureCapacity(ClrwlIndirectBuffers.this.getAllocatedPageCount());
        }

        public void bindForCull(boolean isShadow)
        {
            var handle = isShadow ? shadowLastFrameVisibility.handle() : lastFrameVisibility.handle();
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, ClrwlBufferBindings.base() + ClrwlBufferBindings.VISIBILITY, handle);
        }

        public void delete()
        {
            lastFrameVisibility.delete();
            shadowLastFrameVisibility.delete();
        }
    }

    public class DrawSnapshot
    {
        private long drawInstanceIndexCpy;
        private final long drawInstanceIndexSize;

        private long modelCpy;
        private final long modelSize;

        private long drawCpy;
        private final long drawSize;

        private DrawSnapshot()
        {
            this.drawInstanceIndexSize = drawInstanceIndex.byteCapacity();
            this.modelSize = model.byteCapacity();
            this.drawSize = draw.byteCapacity();

            this.drawInstanceIndexCpy = saveBuffer(drawInstanceIndex, this.drawInstanceIndexSize);
            this.modelCpy = saveBuffer(model, this.modelSize);
            this.drawCpy = saveBuffer(draw, this.drawSize);
        }

        private static long saveBuffer(ResizableStorageArray buffer, long size)
        {
            if (size == 0)
            {
                return 0;
            }

            long ptr = MemoryUtil.nmemAlloc(size);
            GL46.nglGetNamedBufferSubData(buffer.handle(), 0, size, ptr);

            return ptr;
        }

        public void applyAndConsume(StagingBuffer buffer)
        {
            if (drawInstanceIndexCpy > 0)
            {
                buffer.enqueueCopy(drawInstanceIndexCpy, drawInstanceIndexSize, drawInstanceIndex.handle(), 0);
                MemoryUtil.nmemFree(drawInstanceIndexCpy);
                drawInstanceIndexCpy = 0;
            }

            if (modelCpy > 0)
            {
                buffer.enqueueCopy(modelCpy, modelSize, model.handle(), 0);
                MemoryUtil.nmemFree(modelCpy);
                modelCpy = 0;
            }

            if (drawCpy > 0)
            {
                buffer.enqueueCopy(drawCpy, drawSize, draw.handle(), 0);
                MemoryUtil.nmemFree(drawCpy);
                drawCpy = 0;
            }
        }

        public void destroy()
        {
            if (drawInstanceIndexCpy > 0)
            {
                MemoryUtil.nmemFree(drawInstanceIndexCpy);
                drawInstanceIndexCpy = 0;
            }

            if (modelCpy > 0)
            {
                MemoryUtil.nmemFree(modelCpy);
                modelCpy = 0;
            }

            if (drawCpy > 0)
            {
                MemoryUtil.nmemFree(drawCpy);
                drawCpy = 0;
            }
        }
    }
}
