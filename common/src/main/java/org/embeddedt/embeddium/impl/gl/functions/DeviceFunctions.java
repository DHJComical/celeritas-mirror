package org.embeddedt.embeddium.impl.gl.functions;

import org.embeddedt.embeddium.impl.gl.device.RenderDevice;

public record DeviceFunctions(BufferStorageFunctions bufferStorageFunctions,
                              MultidrawFunctions multidrawFunctions,
                              BufferCopyFunctions bufferCopyFunctions,
                              BufferMapRangeFunctions bufferMapRangeFunctions) {
    public DeviceFunctions(RenderDevice device) {
        this(
                BufferStorageFunctions.pickBest(device),
                MultidrawFunctions.pickBest(device),
                BufferCopyFunctions.pickBest(device),
                BufferMapRangeFunctions.pickBest(device)
        );
    }
}
