package org.embeddedt.embeddium.impl.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;

public class TextureUtil {

    public static int getLightTextureId() {
        //? if >=1.17 {
        return RenderSystem.getShaderTexture(2);
        //?} else
        /*return 2;*/
    }

    public static int getBlockTextureId() {
        //? if >=1.17 {
        return RenderSystem.getShaderTexture(0);
        //?} else
        /*return 0;*/
    }
}
