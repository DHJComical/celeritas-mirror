package org.embeddedt.embeddium.impl.mixin.core;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import org.embeddedt.embeddium.impl.blaze3d.CeleritasCommandEncoder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = { "com/mojang/blaze3d/opengl/GlCommandEncoder"} )
public abstract class GlCommandEncoderMixin implements CeleritasCommandEncoder {
    @Shadow
    protected abstract void applyPipelineState(RenderPipeline pipeline);

    @Override
    public void celeritas$configureForPipeline(RenderPipeline renderPipeline) {
        this.applyPipelineState(renderPipeline);
    }
}
