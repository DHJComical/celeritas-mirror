package org.embeddedt.embeddium.impl.mixin.features.textures.quadtree;

import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.embeddedt.embeddium.impl.render.texture.TextureAtlasExtended;
import org.embeddedt.embeddium.impl.util.collections.QuadTree;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

@Mixin(TextureAtlas.class)
public class MixinTextureAtlas implements TextureAtlasExtended {
    @Shadow
    private int width;
    @Shadow
    private int height;

    @Shadow
    private List<SpriteContents> sprites;

    @Shadow
    private Map<ResourceLocation, TextureAtlasSprite> texturesByName;

    private QuadTree<TextureAtlasSprite> celeritas$quadTree;

    @Inject(method = "upload", at = @At("RETURN"))
    private void generateQuadTree(SpriteLoader.Preparations preparations, CallbackInfo ci) {
        QuadTree.Rect2i treeRect = new QuadTree.Rect2i(0, 0, this.width, this.height);
        int minSize = this.sprites.stream().mapToInt(c -> Math.max(c.width(), c.height())).min().orElseThrow();
        this.celeritas$quadTree = new QuadTree<>(treeRect, minSize);
        for (TextureAtlasSprite sprite : this.texturesByName.values()) {
            this.celeritas$quadTree.insert(sprite, s -> new QuadTree.Rect2i(s.getX(), s.getY(), s.contents().width(), s.contents().height()));
        }
    }

    @Override
    public QuadTree<TextureAtlasSprite> celeritas$getQuadTree() {
        return this.celeritas$quadTree;
    }

    @Override
    public TextureAtlasSprite celeritas$findFromUV(float u, float v) {
        int x = (int)(u * this.width), y = (int)(v * this.height);

        return this.celeritas$quadTree.find(x, y);
    }
}
