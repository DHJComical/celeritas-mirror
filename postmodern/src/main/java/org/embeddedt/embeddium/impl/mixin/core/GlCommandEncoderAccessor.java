package org.embeddedt.embeddium.impl.mixin.core;

import com.mojang.blaze3d.opengl.GlCommandEncoder;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GlCommandEncoder.class)
public interface GlCommandEncoderAccessor {
    @Invoker("applyPipelineState")
    void invokeApplyPipelineState(RenderPipeline pipeline);
}
