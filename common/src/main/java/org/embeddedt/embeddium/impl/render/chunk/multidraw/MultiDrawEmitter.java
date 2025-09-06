package org.embeddedt.embeddium.impl.render.chunk.multidraw;

import org.embeddedt.embeddium.impl.gl.device.CommandList;
import org.embeddedt.embeddium.impl.gl.tessellation.GlPrimitiveType;
import org.embeddedt.embeddium.impl.gl.tessellation.GlTessellation;

public interface MultiDrawEmitter {
    void addDrawCommands(long pMeshData, int facingMask, int indexPointerMask);
    void executeBatch(CommandList commandList, GlTessellation tessellation, GlPrimitiveType primitiveType);
    boolean isEmpty();
    int getIndexBufferSize();
    void clear();
    void delete();
}
