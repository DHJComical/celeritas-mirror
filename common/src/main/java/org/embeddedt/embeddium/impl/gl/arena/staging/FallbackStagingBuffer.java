package org.embeddedt.embeddium.impl.gl.arena.staging;

import org.embeddedt.embeddium.impl.gl.buffer.GlBuffer;
import org.embeddedt.embeddium.impl.gl.buffer.GlBufferUsage;
import org.embeddedt.embeddium.impl.gl.buffer.GlMutableBuffer;
import org.embeddedt.embeddium.impl.gl.device.CommandList;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.taumc.celeritas.lwjgl.LWJGLServiceProvider.LWJGL;

public class FallbackStagingBuffer implements StagingBuffer {
    private static final long MIN_SCRATCH_CAPACITY = 1024 * 1024; // 1 MB

    private static final long DEFAULT_MAX_SCRATCH_CAPACITY = 1024 * 1024 * 16; // 16 MB

    private final GlMutableBuffer fallbackBufferObject;

    private final List<CopyCommand> pendingCopies = new ArrayList<>();

    private final long maxScratchCapacity;

    private long scratch;
    private long scratchCapacity;
    private long scratchUsed;

    public FallbackStagingBuffer(CommandList commandList) {
        this(commandList, DEFAULT_MAX_SCRATCH_CAPACITY);
    }

    public FallbackStagingBuffer(CommandList commandList, long maxScratchCapacity) {
        this.fallbackBufferObject = commandList.createMutableBuffer();
        this.maxScratchCapacity = maxScratchCapacity;
    }

    @Override
    public void enqueueCopy(CommandList commandList, ByteBuffer data, GlBuffer dst, long writeOffset) {
        int length = data.remaining();

        if (length == 0) {
            return;
        }

        if (this.scratchUsed + length > this.maxScratchCapacity) {
            this.flush(commandList);

            // Too large to fit even in an empty scratch buffer, so upload it straight to the staging buffer
            if (length > this.maxScratchCapacity) {
                commandList.uploadData(this.fallbackBufferObject, data, GlBufferUsage.STREAM_COPY);
                commandList.copyBufferSubData(this.fallbackBufferObject, dst, 0, writeOffset, length);
                commandList.allocateStorage(this.fallbackBufferObject, 0L, GlBufferUsage.STREAM_COPY);
                return;
            }
        }

        this.ensureScratchCapacity(this.scratchUsed + length);

        long readOffset = this.scratchUsed;
        LWJGL.memCopy(LWJGL.memAddress(data), this.scratch + readOffset, length);
        this.scratchUsed += length;

        // Reads are always contiguous since scratch is filled sequentially, so a copy can be merged into the
        // previous one whenever its destination continues where the previous one ended
        if (!this.pendingCopies.isEmpty()) {
            CopyCommand last = this.pendingCopies.get(this.pendingCopies.size() - 1);

            if (last.buffer == dst && last.writeOffset + last.bytes == writeOffset) {
                last.bytes += length;
                return;
            }
        }

        this.pendingCopies.add(new CopyCommand(dst, readOffset, writeOffset, length));
    }

    @Override
    public void flush(CommandList commandList) {
        if (this.pendingCopies.isEmpty()) {
            return;
        }

        commandList.uploadData(this.fallbackBufferObject, this.scratch, this.scratchUsed, GlBufferUsage.STREAM_COPY);

        for (CopyCommand command : this.pendingCopies) {
            commandList.copyBufferSubData(this.fallbackBufferObject, command.buffer, command.readOffset, command.writeOffset, command.bytes);
        }

        // Release the staging storage so a burst of uploads doesn't keep driver memory allocated
        commandList.allocateStorage(this.fallbackBufferObject, 0L, GlBufferUsage.STREAM_COPY);

        this.pendingCopies.clear();
        this.scratchUsed = 0;
    }

    private void ensureScratchCapacity(long required) {
        if (required <= this.scratchCapacity) {
            return;
        }

        long capacity = Math.max(MIN_SCRATCH_CAPACITY, this.scratchCapacity);

        while (capacity < required) {
            capacity *= 2;
        }

        this.resizeScratch(Math.min(capacity, this.maxScratchCapacity));
    }

    private void resizeScratch(long capacity) {
        long ptr = LWJGL.nmemRealloc(this.scratch, capacity);

        if (ptr == 0L) {
            throw new OutOfMemoryError("Failed to allocate " + capacity + " bytes for staging");
        }

        this.scratch = ptr;
        this.scratchCapacity = capacity;
    }

    @Override
    public void delete(CommandList commandList) {
        commandList.deleteBuffer(this.fallbackBufferObject);
        this.pendingCopies.clear();

        if (this.scratch != 0L) {
            LWJGL.nmemFree(this.scratch);
            this.scratch = 0L;
            this.scratchCapacity = 0;
            this.scratchUsed = 0;
        }
    }

    @Override
    public void flip() {

    }

    @Override
    public String toString() {
        return "Fallback";
    }

    private static final class CopyCommand {
        private final GlBuffer buffer;
        private final long readOffset;
        private final long writeOffset;

        private long bytes;

        private CopyCommand(GlBuffer buffer, long readOffset, long writeOffset, long bytes) {
            this.buffer = buffer;
            this.readOffset = readOffset;
            this.writeOffset = writeOffset;
            this.bytes = bytes;
        }
    }
}
