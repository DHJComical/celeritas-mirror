package org.embeddedt.embeddium.impl.render.chunk.multidraw;

import org.embeddedt.embeddium.impl.gl.device.CommandList;
import org.embeddedt.embeddium.impl.gl.device.DrawCommandList;
import org.embeddedt.embeddium.impl.gl.device.MultiDrawBatch;
import org.embeddedt.embeddium.impl.gl.tessellation.GlIndexType;
import org.embeddedt.embeddium.impl.gl.tessellation.GlPrimitiveType;
import org.embeddedt.embeddium.impl.gl.tessellation.GlTessellation;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.embeddedt.embeddium.impl.render.chunk.data.SectionRenderDataUnsafe;
import static org.taumc.celeritas.lwjgl.LWJGLServiceProvider.LWJGL;

public record DirectMultiDrawEmitter(MultiDrawBatch batch) implements MultiDrawEmitter {
    public DirectMultiDrawEmitter() {
        this(new MultiDrawBatch(MAX_COMMAND_COUNT));
    }

    @Override
    @SuppressWarnings("IntegerMultiplicationImplicitCastToLong")
    public void addDrawCommands(long pMeshData, int mask, int indexPointerMask) {
        var batch = this.batch;
        final var pBaseVertex = batch.pBaseVertex;
        final var pElementCount = batch.pElementCount;
        final var pElementPointer = batch.pElementPointer;

        int size = batch.size;

        for (int facing = 0; facing < ModelQuadFacing.COUNT; facing++) {
            LWJGL.memPutInt(pBaseVertex + (size << 2), SectionRenderDataUnsafe.getVertexOffset(pMeshData, facing));
            LWJGL.memPutInt(pElementCount + (size << 2), SectionRenderDataUnsafe.getElementCount(pMeshData, facing));
            LWJGL.memPutAddress(pElementPointer + (size << 3), SectionRenderDataUnsafe.getIndexOffset(pMeshData, facing) & indexPointerMask);

            size += (mask >> facing) & 1;
        }

        batch.size = size;
    }

    @Override
    public void executeBatch(CommandList commandList, GlTessellation tessellation, GlPrimitiveType primitiveType) {
        try (DrawCommandList drawCommandList = commandList.beginTessellating(tessellation)) {
            drawCommandList.multiDrawElementsBaseVertex(batch, primitiveType, GlIndexType.UNSIGNED_INT);
        }
    }

    @Override
    public int getIndexBufferSize() {
        return this.batch.getIndexBufferSize();
    }

    @Override
    public boolean isEmpty() {
        return this.batch.isEmpty();
    }

    @Override
    public void clear() {
        this.batch.clear();
    }

    @Override
    public void delete() {
        this.batch.delete();
    }
}
