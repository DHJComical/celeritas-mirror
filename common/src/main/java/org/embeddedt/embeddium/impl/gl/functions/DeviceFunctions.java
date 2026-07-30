package org.embeddedt.embeddium.impl.gl.functions;

import org.embeddedt.embeddium.impl.gl.device.RenderDevice;

public record DeviceFunctions(BufferCopyFunctions bufferCopyFunctions,
                              BufferMapRangeFunctions bufferMapRangeFunctions) {
    public DeviceFunctions(RenderDevice device) {
        this(
                BufferCopyFunctions.pickBest(device),
                BufferMapRangeFunctions.pickBest(device)
        );
    }
}
