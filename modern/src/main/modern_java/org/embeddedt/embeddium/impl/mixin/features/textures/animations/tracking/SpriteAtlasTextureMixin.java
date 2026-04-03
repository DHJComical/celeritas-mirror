package org.embeddedt.embeddium.impl.mixin.features.textures.animations.tracking;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.embeddedt.embeddium.api.render.texture.SpriteUtil;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TextureAtlas.class)
public class SpriteAtlasTextureMixin {
    @ModifyReturnValue(method = "getSprite", at = @At("RETURN"))
    private TextureAtlasSprite preReturnSprite(TextureAtlasSprite sprite) {
        if (sprite != null) {
            SpriteUtil.markSpriteActive(sprite);
        }

        return sprite;
    }
}
