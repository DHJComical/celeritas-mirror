package org.embeddedt.embeddium.impl.render.chunk.multidraw;

import org.embeddedt.embeddium.impl.gl.buffer.GlBufferTarget;
import org.embeddedt.embeddium.impl.gl.buffer.GlBufferUsage;
import org.embeddedt.embeddium.impl.gl.buffer.GlMutableBuffer;
import org.embeddedt.embeddium.impl.gl.device.CommandList;
import org.embeddedt.embeddium.impl.gl.device.DrawCommandList;
import org.embeddedt.embeddium.impl.gl.tessellation.GlIndexType;
import org.embeddedt.embeddium.impl.gl.tessellation.GlPrimitiveType;
import org.embeddedt.embeddium.impl.gl.tessellation.GlTessellation;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.embeddedt.embeddium.impl.render.chunk.data.SectionRenderDataUnsafe;
import org.lwjgl.system.MemoryUtil;

/**
 * A multidraw emitter that uses indirect rendering to exploit hardware acceleration, which
 * reduces CPU overhead on some platforms.
 * @author Ven
 */
public class IndirectMultiDrawEmitter implements MultiDrawEmitter {
    // uint  count;
    // uint  instanceCount;
    // uint  firstIndex;
    // int  baseVertex;
    // uint  baseInstance;
    private static final int COMMAND_SIZE = 4 * 5;
    private static final int BUFFER_SIZE = MultiDrawEmitter.MAX_COMMAND_COUNT * COMMAND_SIZE;

    private final long indirectBuffer;
    private int numCommands;
    private final GlMutableBuffer indirectBufferGpu;

    public IndirectMultiDrawEmitter() {
        this.indirectBuffer = MemoryUtil.nmemAlignedAlloc(32, BUFFER_SIZE);
        if (this.indirectBuffer == MemoryUtil.NULL) {
            throw new OutOfMemoryError("Failed to allocate indirect buffer");
        }
        this.prefillConstants();
        this.indirectBufferGpu = new GlMutableBuffer();
    }

    private void prefillConstants() {
        // Prefill constants
        long ptr = this.indirectBuffer;
        for (int i = 0; i < MultiDrawEmitter.MAX_COMMAND_COUNT; i++) {
            MemoryUtil.memPutInt(ptr + 4L, 1); // instanceCount
            MemoryUtil.memPutInt(ptr + 16L, 0); // baseInstance
            ptr += COMMAND_SIZE;
        }
    }

    @Override
    public void addDrawCommands(long pMeshData, int facingMask, int indexPointerMask) {
        int size = this.numCommands;

        long basePtr = this.indirectBuffer;

        for (int facing = 0; facing < ModelQuadFacing.COUNT; facing++) {
            long ptr = basePtr + (long)size * COMMAND_SIZE;
            MemoryUtil.memPutInt(ptr + 0L, SectionRenderDataUnsafe.getElementCount(pMeshData, facing)); // count
            int indexOffset = SectionRenderDataUnsafe.getIndexOffset(pMeshData, facing) & indexPointerMask;
            MemoryUtil.memPutInt(ptr + 8L, indexOffset / 4);
            MemoryUtil.memPutInt(ptr + 12L, SectionRenderDataUnsafe.getVertexOffset(pMeshData, facing)); // baseVertex

            size += (facingMask >> facing) & 1;
        }

        this.numCommands = size;
    }

    @Override
    public void executeBatch(CommandList commandList, GlTessellation tessellation, GlPrimitiveType primitiveType) {
        commandList.uploadData(this.indirectBufferGpu, this.indirectBuffer, (long)this.numCommands * COMMAND_SIZE,
                GlBufferUsage.STREAM_DRAW);

        commandList.bindBuffer(GlBufferTarget.DRAW_INDIRECT_BUFFER, this.indirectBufferGpu);
        try (DrawCommandList drawCommandList = commandList.beginTessellating(tessellation)) {
            drawCommandList.multiDrawElementsIndirect(indirectBufferGpu, numCommands, primitiveType, GlIndexType.UNSIGNED_INT);
        }
        commandList.bindBuffer(GlBufferTarget.DRAW_INDIRECT_BUFFER, null);
    }

    @Override
    public boolean isEmpty() {
        return this.numCommands == 0;
    }

    @Override
    public int getIndexBufferSize() {
        int elements = 0;

        long pElementCount = this.indirectBuffer;

        for (var index = 0; index < this.numCommands; index++) {
            elements = Math.max(elements, MemoryUtil.memGetInt(pElementCount));
            pElementCount += COMMAND_SIZE;
        }

        return elements;
    }

    @Override
    public void clear() {
        this.numCommands = 0;
    }

    @Override
    public void delete() {
        MemoryUtil.nmemAlignedFree(this.indirectBuffer);
        this.indirectBufferGpu.delete();
    }
}
