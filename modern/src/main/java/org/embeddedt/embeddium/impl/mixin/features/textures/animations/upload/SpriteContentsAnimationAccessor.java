package org.embeddedt.embeddium.impl.mixin.features.textures.animations.upload;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

//? if >=1.18 {

//? if >=1.20 {
import net.minecraft.client.renderer.texture.SpriteContents;

@Mixin(SpriteContents.AnimatedTexture.class)
//?} else {
/*import net.minecraft.client.renderer.texture.TextureAtlasSprite;

@Mixin(TextureAtlasSprite.AnimatedTexture.class)
*///?}
public interface SpriteContentsAnimationAccessor {
    @Accessor
    //? if >=1.20 {
    List<SpriteContents.FrameInfo> getFrames();
    //?} else
    /*List<TextureAtlasSprite.FrameInfo> getFrames();*/

    @Accessor
    int getFrameRowSize();
}

//?}