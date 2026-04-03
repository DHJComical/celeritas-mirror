package org.embeddedt.embeddium.impl.mixin.features.textures.mipmaps;

//? if >=1.19.3 {
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.embeddedt.embeddium.impl.render.chunk.sprite.SpriteTransparencyLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(TextureAtlasSprite.class)
public abstract class TextureAtlasSpriteMixin implements SpriteTransparencyLevel.Holder {
    @Shadow
    public abstract SpriteContents contents();

    @Override
    public SpriteTransparencyLevel embeddium$getTransparencyLevel() {
        return ((SpriteTransparencyLevel.Holder)this.contents()).embeddium$getTransparencyLevel();
    }
}
//?}