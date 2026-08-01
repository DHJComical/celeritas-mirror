package org.embeddedt.embeddium.impl.render.chunk.multidraw;

import org.embeddedt.embeddium.impl.gl.buffer.GlBufferTarget;
import org.embeddedt.embeddium.impl.gl.buffer.GlBufferUsage;
import org.embeddedt.embeddium.impl.gl.buffer.GlMutableBuffer;
import org.embeddedt.embeddium.impl.gl.device.CommandList;
import org.embeddedt.embeddium.impl.gl.device.DrawCommandList;
import org.embeddedt.embeddium.impl.gl.device.MultiDrawBatch;
import org.embeddedt.embeddium.impl.gl.tessellation.GlIndexType;
import org.embeddedt.embeddium.impl.gl.tessellation.GlPrimitiveType;
import org.embeddedt.embeddium.impl.gl.tessellation.GlTessellation;

import static org.taumc.celeritas.lwjgl.LWJGLServiceProvider.LWJGL;
import org.taumc.celeritas.lwjgl.LWJGLServiceProvider;

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
    private final GlMutableBuffer indirectBufferGpu;

    public IndirectMultiDrawEmitter() {
        this.indirectBuffer = LWJGL.nmemAlignedAlloc(32, BUFFER_SIZE);
        if (this.indirectBuffer == LWJGLServiceProvider.NULL) {
            throw new OutOfMemoryError("Failed to allocate indirect buffer");
        }
        this.prefillConstants();
        this.indirectBufferGpu = new GlMutableBuffer();
    }

    private void prefillConstants() {
        // Prefill constants
        long ptr = this.indirectBuffer;
        for (int i = 0; i < MultiDrawEmitter.MAX_COMMAND_COUNT; i++) {
            LWJGL.memPutInt(ptr + 4L, 1); // instanceCount
            LWJGL.memPutInt(ptr + 16L, 0); // baseInstance
            ptr += COMMAND_SIZE;
        }
    }

    /**
     * Copies the data out of a MultiDrawBatch assembled for direct multidraw into the indirect buffer that will
     * be uploaded to the GPU.
     */
    private int buildCommands(MultiDrawBatch batch) {
        int numCommands = batch.size;

        long pBaseVertex = batch.pBaseVertex;
        long pElementCount = batch.pElementCount;
        long pElementPointer = batch.pElementPointer;

        long ptr = this.indirectBuffer;

        for (int i = 0; i < numCommands; i++) {
            LWJGL.memPutInt(ptr + 0L, LWJGL.memGetInt(pElementCount + ((long) i * Integer.BYTES))); // count
            LWJGL.memPutInt(ptr + 8L, (int) (LWJGL.memGetAddress(pElementPointer + ((long) i * 8L)) / 4L)); // firstIndex
            LWJGL.memPutInt(ptr + 12L, LWJGL.memGetInt(pBaseVertex + ((long) i * Integer.BYTES))); // baseVertex
            ptr += COMMAND_SIZE;
        }

        return numCommands;
    }

    @Override
    public void executeBatch(CommandList commandList, GlTessellation tessellation, GlPrimitiveType primitiveType, MultiDrawBatch batch) {
        int numCommands = this.buildCommands(batch);

        commandList.uploadData(this.indirectBufferGpu, this.indirectBuffer, (long)numCommands * COMMAND_SIZE,
                GlBufferUsage.STREAM_DRAW);

        commandList.bindBuffer(GlBufferTarget.DRAW_INDIRECT_BUFFER, this.indirectBufferGpu);
        try (DrawCommandList drawCommandList = commandList.beginTessellating(tessellation)) {
            drawCommandList.multiDrawElementsIndirect(indirectBufferGpu, numCommands, primitiveType, GlIndexType.UNSIGNED_INT);
        }
        commandList.bindBuffer(GlBufferTarget.DRAW_INDIRECT_BUFFER, null);
    }

    @Override
    public void delete() {
        LWJGL.nmemAlignedFree(this.indirectBuffer);
        this.indirectBufferGpu.delete();
    }
}
