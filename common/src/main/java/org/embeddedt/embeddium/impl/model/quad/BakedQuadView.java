package org.embeddedt.embeddium.impl.model.quad;

import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.embeddedt.embeddium.impl.render.chunk.sprite.SpriteTransparencyLevel;
import org.jetbrains.annotations.Nullable;

public interface BakedQuadView extends ModelQuadView {
    ModelQuadFacing getNormalFace();

    boolean hasShade();

    void addFlags(int flags);

    int getVerticesCount();

    @Nullable SpriteTransparencyLevel getTransparencyLevel();

    static BakedQuadView of(Object o) {
        return (BakedQuadView)o;
    }
}
