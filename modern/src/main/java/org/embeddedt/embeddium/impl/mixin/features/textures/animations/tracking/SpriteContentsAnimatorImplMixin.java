package org.embeddedt.embeddium.impl.mixin.features.textures.animations.tracking;

//? if >=1.17 {
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.embeddedt.embeddium.impl.Celeritas;
import org.embeddedt.embeddium.impl.render.texture.SpriteContentsExtended;
//? if >=1.20
import net.minecraft.client.renderer.texture.SpriteContents;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

//? if >=1.20
@Mixin(SpriteContents.Ticker.class)
//? if <1.20
/*@Mixin(TextureAtlasSprite.AnimatedTexture.class)*/
public class SpriteContentsAnimatorImplMixin {
    @Shadow
    int subFrame;
    @Shadow
    int frame;

    //? if >=1.20 {
    @Shadow
    @Final
    SpriteContents.AnimatedTexture animationInfo;
    @Unique
    private SpriteContents parent;
    //?} else {
    /*@Shadow
    @Final
    private List<TextureAtlasSprite.FrameInfo> frames;
    @Unique
    private TextureAtlasSprite parent;
    *///?}

    //? if >=1.20 {
    @Inject(method = "<init>", at = @At("RETURN"))
    public void assignParent(SpriteContents spriteContents, SpriteContents.AnimatedTexture animation, SpriteContents.InterpolationData interpolation, CallbackInfo ci) {
        this.parent = spriteContents;
    }
    private static final String TICK_METHOD = "tickAndUpload";
    //?} else {
    /*@Inject(method = "<init>", at = @At("RETURN"))
    public void assignParent(TextureAtlasSprite spriteContents, List<TextureAtlasSprite.FrameInfo> pFrames, int pFrameRowSize, TextureAtlasSprite.InterpolationData pInterpolationData, CallbackInfo ci) {
        this.parent = spriteContents;
    }
    private static final String TICK_METHOD = "tick";
    *///?}

    @Inject(method = TICK_METHOD, at = @At("HEAD"), cancellable = true)
    private void preTick(CallbackInfo ci) {
        SpriteContentsExtended parent = (SpriteContentsExtended) this.parent;

        boolean onDemand = Celeritas.options().performance.animateOnlyVisibleTextures;

        if (onDemand && !parent.sodium$isActive()) {
            this.subFrame++;
            //? if >=1.20 {
            List<SpriteContents.FrameInfo> frames = ((SpriteContentsAnimationAccessor)this.animationInfo).getFrames();
            int curFrameTime = ((SpriteContentsAnimationFrameAccessor)(Object)frames.get(this.frame)).getTime();
            //?} else {
            /*List<TextureAtlasSprite.FrameInfo> frames = this.frames;
            int curFrameTime = frames.get(this.frame).time;
            *///?}
            if (this.subFrame >= curFrameTime) {
                this.frame = (this.frame + 1) % frames.size();
                this.subFrame = 0;
            }
            ci.cancel();
        }
    }

    @Inject(method = TICK_METHOD, at = @At("TAIL"))
    private void postTick(CallbackInfo ci) {
        SpriteContentsExtended parent = (SpriteContentsExtended) this.parent;
        parent.sodium$setActive(false);
    }
}
//?}