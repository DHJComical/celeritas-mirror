package org.embeddedt.embeddium.impl.render.frapi;

import com.mojang.blaze3d.vertex.PoseStack;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildBuffers;
import org.embeddedt.embeddium.impl.modern.render.chunk.compile.pipeline.BlockRenderContext;
//$ rng_import
import net.minecraft.util.RandomSource;

public interface FRAPIRenderHandler {
    boolean INDIGO_PRESENT = isIndigoPresent();

    private static boolean isIndigoPresent() {
        boolean indigoPresent = false;
        try {
            Class.forName("net.fabricmc.fabric.impl.client.indigo.renderer.render.BlockRenderContext");
            indigoPresent = true;
        } catch(Throwable ignored) {}
        return indigoPresent;
    }

    void reset();

    void renderEmbeddium(BlockRenderContext ctx, ChunkBuildBuffers buffers, PoseStack mStack, /*$ rng >>*/ RandomSource random);
}
