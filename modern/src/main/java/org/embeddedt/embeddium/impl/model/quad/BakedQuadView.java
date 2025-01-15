package org.embeddedt.embeddium.impl.model.quad;

import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.embeddedt.embeddium.impl.render.chunk.sprite.SpriteTransparencyLevel;

import javax.annotation.Nullable;

public interface BakedQuadView extends ModelQuadView {
    ModelQuadFacing getNormalFace();

    boolean hasShade();

    void addFlags(int flags);

    @Nullable SpriteTransparencyLevel getTransparencyLevel();
}
