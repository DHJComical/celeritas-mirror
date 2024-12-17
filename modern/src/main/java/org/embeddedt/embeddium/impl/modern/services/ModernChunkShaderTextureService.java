package org.embeddedt.embeddium.impl.modern.services;

import com.mojang.blaze3d.platform.GlStateManager;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderTextureService;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderTextureSlot;
import org.embeddedt.embeddium.impl.util.TextureUtil;
import org.lwjgl.opengl.GL32C;

public class ModernChunkShaderTextureService implements ChunkShaderTextureService {
    @Override
    public int bindAndGetUniformValue(ChunkShaderTextureSlot textureSlot) {
        int textureId = switch(textureSlot) {
            case BLOCK -> TextureUtil.getBlockTextureId();
            case LIGHT -> TextureUtil.getLightTextureId();
        };
        //? if >=1.17 {
        GlStateManager._activeTexture(GL32C.GL_TEXTURE0 + textureSlot.ordinal());
        GlStateManager._bindTexture(textureId);

        return textureSlot.ordinal();
        //?} else {
        /*return textureId;
         *///?}
    }
}
