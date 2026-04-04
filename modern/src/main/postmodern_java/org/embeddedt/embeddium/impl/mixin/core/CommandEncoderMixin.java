package org.embeddedt.embeddium.impl.mixin.core;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.CommandEncoderBackend;
import org.embeddedt.embeddium.impl.blaze3d.CeleritasCommandEncoder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(CommandEncoder.class)
public class CommandEncoderMixin implements CeleritasCommandEncoder {
    @Shadow
    @Final
    private CommandEncoderBackend backend;

    private CeleritasCommandEncoder getExtendedBackendOrThrow() {
        if (this.backend instanceof CeleritasCommandEncoder encoder) {
            return encoder;
        } else {
            throw new IllegalStateException("Unexpected command encoder class without Celeritas support: " + backend.getClass().getName());
        }
    }

    @Override
    public void celeritas$configureForPipeline(RenderPipeline renderPipeline) {
        getExtendedBackendOrThrow().celeritas$configureForPipeline(renderPipeline);
    }
}
