package org.embeddedt.embeddium.impl.gl.functions;

import org.embeddedt.embeddium.impl.gl.device.RenderDevice;

/**
 * @param maxTextureBufferTexels the number of texels a texture buffer may address, or 0 if texture buffers are unsupported
 */
public record DeviceFunctions(BufferCopyFunctions bufferCopyFunctions,
                              BufferMapRangeFunctions bufferMapRangeFunctions,
                              TextureBufferFunctions textureBufferFunctions,
                              int maxTextureBufferTexels) {
    public DeviceFunctions(RenderDevice device) {
        this(
                BufferCopyFunctions.pickBest(device),
                BufferMapRangeFunctions.pickBest(device),
                TextureBufferFunctions.pickBest(device)
        );
    }

    private DeviceFunctions(BufferCopyFunctions bufferCopyFunctions,
                            BufferMapRangeFunctions bufferMapRangeFunctions,
                            TextureBufferFunctions textureBufferFunctions) {
        this(bufferCopyFunctions, bufferMapRangeFunctions, textureBufferFunctions, textureBufferFunctions.queryMaxTexels());
    }
}
