package org.embeddedt.embeddium.api.render.texture;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.embeddedt.embeddium.impl.render.chunk.compile.GlobalChunkBuildContext;
import org.embeddedt.embeddium.impl.modern.render.chunk.compile.ModernChunkBuildContext;
import org.embeddedt.embeddium.impl.render.texture.SpriteContentsExtended;
import org.jetbrains.annotations.Nullable;

public class SpriteUtil {
    public static void markSpriteActive(@Nullable TextureAtlasSprite sprite) {
        if (sprite == null) {
            // Can happen in some cases, for example if a mod passes a BakedQuad with a null sprite
            // to a VertexConsumer that does not have a texture element.
            return;
        }

        //? if >=1.20 {
        ((SpriteContentsExtended) sprite.contents()).sodium$setActive(true);
        //?} else
        /*((SpriteContentsExtended) sprite).sodium$setActive(true);*/

        if(hasAnimation(sprite)) {
            var context = GlobalChunkBuildContext.get();

            if (context instanceof ModernChunkBuildContext modernContext) {
                modernContext.captureAdditionalSprite(sprite);
            }
        }
    }

    public static boolean hasAnimation(TextureAtlasSprite sprite) {
        //? if >=1.20 {
        return sprite != null && ((SpriteContentsExtended) sprite.contents()).sodium$hasAnimation();
        //?} else
        /*return sprite != null && ((SpriteContentsExtended) sprite).sodium$hasAnimation();*/
    }
}
