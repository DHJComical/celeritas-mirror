package org.embeddedt.embeddium.impl.modern.render.chunk.sprite;

//? if >=1.20
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.embeddedt.embeddium.impl.render.chunk.sprite.SpriteTransparencyLevel;

public interface SpriteTransparencyLevelHolder {
    SpriteTransparencyLevel embeddium$getTransparencyLevel();

    static SpriteTransparencyLevel getTransparencyLevel(TextureAtlasSprite sprite) {
        //? if >=1.20 {
        return getTransparencyLevel(sprite.contents());
        //?} else
        /*return ((SpriteTransparencyLevelHolder)sprite).embeddium$getTransparencyLevel();*/
    }

    //? if >=1.20 {
    static SpriteTransparencyLevel getTransparencyLevel(SpriteContents contents) {
        return ((SpriteTransparencyLevelHolder)contents).embeddium$getTransparencyLevel();
    }
    //?}
}
