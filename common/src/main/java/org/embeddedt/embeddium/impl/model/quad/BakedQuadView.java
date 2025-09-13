package org.embeddedt.embeddium.impl.model.quad;

import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.embeddedt.embeddium.impl.render.chunk.sprite.SpriteTransparencyLevel;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface BakedQuadView extends ModelQuadView {
    ModelQuadFacing getNormalFace();

    boolean hasShade();

    void addFlags(int flags);

    int getVerticesCount();

    @Nullable SpriteTransparencyLevel getTransparencyLevel();

    static BakedQuadView of(Object o) {
        return (BakedQuadView)o;
    }

    @SuppressWarnings("unchecked")
    static <T> List<? extends BakedQuadView> ofList(List<T> quads) {
        return (List<? extends BakedQuadView>)(List<?>)quads;
    }
}
