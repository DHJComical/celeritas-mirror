package org.embeddedt.embeddium.impl.render.texture;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.embeddedt.embeddium.impl.util.collections.quadtree.QuadTree;

public interface TextureAtlasExtended {
    QuadTree<TextureAtlasSprite> celeritas$getQuadTree();

    TextureAtlasSprite celeritas$findFromUV(float u, float v);

    int celeritas$getWidth();

    int celeritas$getHeight();

    /**
     * {@return the number of mip levels the atlas was stitched for, or 0 if unknown}
     */
    int celeritas$getMipLevel();

    /**
     * {@return the x, y, width and height of every sprite in atlas texels, four ints per sprite}
     */
    int[] celeritas$getSpriteRects();
}
