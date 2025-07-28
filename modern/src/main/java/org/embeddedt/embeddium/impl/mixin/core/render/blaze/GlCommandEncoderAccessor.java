package org.embeddedt.embeddium.impl.mixin.core.render.blaze;

//? if >=1.21.5 {
/*import com.mojang.blaze3d.opengl.GlCommandEncoder;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GlCommandEncoder.class)
public interface GlCommandEncoderAccessor {
    @Invoker("applyPipelineState")
    void invokeApplyPipelineState(RenderPipeline pipeline);
}
*///?}