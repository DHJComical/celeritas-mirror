package org.embeddedt.embeddium.impl.model.quad;

import net.minecraft.client.renderer.block.model.BakedQuad;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.embeddedt.embeddium.impl.render.chunk.sprite.SpriteTransparencyLevel;

import javax.annotation.Nullable;

public interface BakedQuadView extends ModelQuadView {
    ModelQuadFacing getNormalFace();

    boolean hasShade();

    void addFlags(int flags);

    int getVerticesCount();

    @Nullable SpriteTransparencyLevel getTransparencyLevel();

    static BakedQuadView of(BakedQuad quad) {
        return (BakedQuadView)(Object)quad;
    }
}
