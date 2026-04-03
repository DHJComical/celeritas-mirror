package org.embeddedt.embeddium.impl.render.texture;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.embeddedt.embeddium.impl.util.collections.quadtree.QuadTree;

public interface TextureAtlasExtended {
    QuadTree<TextureAtlasSprite> celeritas$getQuadTree();

    TextureAtlasSprite celeritas$findFromUV(float u, float v);
}
