package org.embeddedt.embeddium.impl.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;

public class TextureUtil {

    public static int getLightTextureId() {
        //? if >=1.17 {
        return RenderSystem.getShaderTexture(2)/*? if >=1.21.5-alpha.25.7.a {*//*.glId()*//*?}*/;
        //?} else
        /*return 2;*/
    }

    public static int getBlockTextureId() {
        //? if >=1.17 {
        return RenderSystem.getShaderTexture(0)/*? if >=1.21.5-alpha.25.7.a {*//*.glId()*//*?}*/;
        //?} else
        /*return 0;*/
    }
}
