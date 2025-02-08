package org.embeddedt.embeddium.impl.mixin.features.textures.animations.tracking;

//? if >=1.20 {
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.embeddedt.embeddium.api.render.texture.SpriteUtil;
import org.embeddedt.embeddium.impl.render.texture.SpriteContentsExtended;
import net.minecraft.client.renderer.texture.SpriteContents;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(SpriteContents.class)
public abstract class SpriteContentsMixin implements SpriteContentsExtended {
    @Shadow
    @Final
    @Nullable
    private SpriteContents.AnimatedTexture animatedTexture;

    @Unique
    private boolean active;

    @Override
    public void sodium$setActive(boolean value) {
        this.active = value;
        //? if shaders {
        net.irisshaders.iris.texture.pbr.PBRSpriteHolder pbrHolder = ((net.irisshaders.iris.texture.pbr.SpriteContentsExtension) this).getPBRHolder();
        if (pbrHolder != null) {
            TextureAtlasSprite normalSprite = pbrHolder.getNormalSprite();
            TextureAtlasSprite specularSprite = pbrHolder.getSpecularSprite();
            if (normalSprite != null) {
                SpriteUtil.markSpriteActive(normalSprite);
            }
            if (specularSprite != null) {
                SpriteUtil.markSpriteActive(specularSprite);
            }
        }
        //?}
    }

    @Override
    public boolean sodium$hasAnimation() {
        return this.animatedTexture != null;
    }

    @Override
    public boolean sodium$isActive() {
        return this.active;
    }
}
//?}
