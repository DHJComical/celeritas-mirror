package org.embeddedt.embeddium.impl.mixin.features.textures.quadtree;

//? if >=1.19.3 {
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteLoader;
//?}
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.embeddedt.embeddium.impl.render.texture.TextureAtlasExtended;
import org.embeddedt.embeddium.impl.util.collections.quadtree.QuadTree;
import org.embeddedt.embeddium.impl.util.collections.quadtree.Rect2i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(TextureAtlas.class)
public class MixinTextureAtlas implements TextureAtlasExtended {
    //? if >=1.19.3 {
    @Shadow
    private int width;
    @Shadow
    private int height;
    //?} else {
    /*@Unique
    private int width, height;
    *///?}

    @Shadow
    private Map<ResourceLocation, TextureAtlasSprite> texturesByName;

    private QuadTree<TextureAtlasSprite> celeritas$quadTree = QuadTree.empty();

    //? if >=1.19.3 {
    @Inject(method = "upload", at = @At("RETURN"))
    private void generateQuadTree(SpriteLoader.Preparations preparations, CallbackInfo ci) {
    //?} else {
    /*@Inject(method = "reload", at = @At("RETURN"))
    private void generateQuadTree(TextureAtlas.Preparations preparations, CallbackInfo ci) {
        int width = preparations.width;
        int height = preparations.height;
        this.width = width;
        this.height = height;
    *///?}
        Rect2i treeRect = new Rect2i(0, 0, width, height);
        int minSize = this.texturesByName.values().stream()
                //? if >=1.19.3 {
                .mapToInt(c -> Math.max(c.contents().width(), c.contents().height()))
                //?} else
                /*.mapToInt(c -> Math.max(c.getWidth(), c.getHeight()))*/
                .min().orElse(0);
        this.celeritas$quadTree = new QuadTree<>(treeRect, minSize,
                this.texturesByName.values(),
                s ->
                //? if >=1.19.3 {
                new Rect2i(s.getX(), s.getY(), s.contents().width(), s.contents().height())
                //?} else
                /*new Rect2i(s.x, s.y, s.getWidth(), s.getHeight())*/
                );
    }

    @Inject(method = "clearTextureData", at = @At("RETURN"))
    private void clearQuadTree(CallbackInfo ci) {
        this.celeritas$quadTree = QuadTree.empty();
    }

    @Override
    public QuadTree<TextureAtlasSprite> celeritas$getQuadTree() {
        return this.celeritas$quadTree;
    }

    @Override
    public TextureAtlasSprite celeritas$findFromUV(float u, float v) {
        int x = Math.round(u * this.width), y = Math.round(v * this.height);

        return this.celeritas$quadTree.find(x, y);
    }
}
