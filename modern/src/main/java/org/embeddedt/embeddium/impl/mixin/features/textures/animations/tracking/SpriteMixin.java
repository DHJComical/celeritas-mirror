package org.embeddedt.embeddium.impl.mixin.features.textures.animations.tracking;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.embeddedt.embeddium.impl.render.texture.SpriteContentsExtended;
import org.embeddedt.embeddium.api.render.texture.SpriteUtil;
//? if >=1.20
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TextureAtlasSprite.class)
public class SpriteMixin
    //? if >=1.20 {
{
    @Shadow
    @Final
    private SpriteContents contents;
    //?} else {
/*implements SpriteContentsExtended {

    //? if >=1.17 {
    @Shadow
    @Final
    @Nullable
    private TextureAtlasSprite.AnimatedTexture animatedTexture;
    //?} else {
    /^@Shadow
    public boolean isAnimation() { throw new AssertionError(); }
    ^///?}

    @Unique
    private boolean active;

    @Override
    public void sodium$setActive(boolean value) {
        this.active = value;
    }

    @Override
    public boolean sodium$hasAnimation() {
        //? if >=1.17 {
        return this.animatedTexture != null;
        //?} else
        /^return isAnimation();^/
    }

    @Override
    public boolean sodium$isActive() {
        return this.active;
    }
    *///?}

    /**
     * @author embeddedt
     * @reason Mark sprite as active for animation when U0 coordinate is retrieved. This catches some more render
     * paths not caught by the other mixins.
     */
    @ModifyReturnValue(method = "getU0", at = @At("RETURN"))
    private float embeddium$markActive(float f) {
        SpriteUtil.markSpriteActive((TextureAtlasSprite)(Object)this);
        return f;
    }

    /**
     * @author embeddedt
     * @reason Mark sprite as active for animation when U coordinate is retrieved. This catches some more render
     * paths not caught by the other mixins.
     */
    @ModifyReturnValue(method = "getU", at = @At("RETURN"))
    private float embeddium$markActiveInterpolated(float f) {
        SpriteUtil.markSpriteActive((TextureAtlasSprite)(Object)this);
        return f;
    }
}
