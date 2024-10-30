package org.embeddedt.embeddium.impl.mixin.features.textures.animations.tracking;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.embeddedt.embeddium.impl.render.texture.SpriteUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Function;

@Mixin(GuiGraphics.class)
public class DrawContextMixin {
    //? if <1.21.2 {
    @Inject(method = "blit(IIIIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V", at = @At("HEAD"))
    private void preDrawSprite(int x, int y, int z, int width, int height, TextureAtlasSprite sprite, CallbackInfo ci)
    //?} else {
    /*@Inject(method = "blitSprite(Ljava/util/function/Function;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;IIIIIIIII)V", at = @At("HEAD"))
    private void preDrawSprite(Function<ResourceLocation, RenderType> function, TextureAtlasSprite sprite, int i, int j, int k, int l, int m, int n, int o, int p, int q, CallbackInfo ci)
    *///?}
    {
        SpriteUtil.markSpriteActive(sprite);
    }

    //? if <1.21.2 {
    @Inject(method = "blit(IIIIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;FFFF)V", at = @At("HEAD"))
    private void preDrawSprite(int x, int y, int z, int width, int height, TextureAtlasSprite sprite, float red, float green, float blue, float alpha, CallbackInfo ci)
    //?} else {
    /*@Inject(method = "blitSprite(Ljava/util/function/Function;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;IIIII)V", at = @At("HEAD"))
    private void preDrawSprite(Function<ResourceLocation, RenderType> function, TextureAtlasSprite sprite, int i, int j, int k, int l, int m, CallbackInfo ci)
    {
        SpriteUtil.markSpriteActive(sprite);
    }
    *///?}
}
