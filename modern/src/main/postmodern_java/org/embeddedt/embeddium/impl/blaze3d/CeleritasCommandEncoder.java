package org.embeddedt.embeddium.impl.blaze3d;

import com.mojang.blaze3d.pipeline.RenderPipeline;

public interface CeleritasCommandEncoder {
    void celeritas$configureForPipeline(RenderPipeline renderPipeline);

    /**
     * Forgets the program and pipeline state the encoder last applied, so that the next draw applies its own. Must be
     * called after changing GL state without the encoder, since it skips state it believes is already applied.
     */
    void celeritas$invalidateAppliedState();
}
