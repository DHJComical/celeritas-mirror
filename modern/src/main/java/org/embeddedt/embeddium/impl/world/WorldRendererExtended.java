package org.embeddedt.embeddium.impl.world;

import net.minecraft.client.renderer.LevelRenderer;
import org.embeddedt.embeddium.impl.render.CeleritasWorldRenderer;

public interface WorldRendererExtended {
    CeleritasWorldRenderer sodium$getWorldRenderer();

    static CeleritasWorldRenderer forVanilla(LevelRenderer levelRenderer) {
        return ((WorldRendererExtended)levelRenderer).sodium$getWorldRenderer();
    }
}
