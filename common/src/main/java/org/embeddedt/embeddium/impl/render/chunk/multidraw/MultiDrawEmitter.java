package org.embeddedt.embeddium.impl.render.chunk.multidraw;

import org.embeddedt.embeddium.impl.gl.device.CommandList;
import org.embeddedt.embeddium.impl.gl.tessellation.GlPrimitiveType;
import org.embeddedt.embeddium.impl.gl.tessellation.GlTessellation;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.embeddedt.embeddium.impl.render.chunk.region.RenderRegion;

public interface MultiDrawEmitter {
    int MAX_COMMAND_COUNT = (ModelQuadFacing.COUNT * RenderRegion.REGION_SIZE) + 1;

    void addDrawCommands(long pMeshData, int facingMask, int indexPointerMask);
    void executeBatch(CommandList commandList, GlTessellation tessellation, GlPrimitiveType primitiveType);
    boolean isEmpty();
    int getIndexBufferSize();
    void clear();
    void delete();
}
