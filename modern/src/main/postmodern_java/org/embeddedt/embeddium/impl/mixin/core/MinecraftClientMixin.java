package org.embeddedt.embeddium.impl.mixin.core;

import net.minecraft.client.Minecraft;
import org.embeddedt.embeddium.impl.Celeritas;
import org.embeddedt.embeddium.impl.render.frame.RenderAheadManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftClientMixin {
    @Unique
    private final RenderAheadManager celeritas$renderAheadManager = new RenderAheadManager();

    @Inject(method = "runTick", at = @At("HEAD"))
    private void celeritas$waitForGpu(boolean advanceGameTime, CallbackInfo ci) {
        this.celeritas$renderAheadManager.startFrame(Celeritas.options().advanced.cpuRenderAheadLimit);
    }

    @Inject(method = "runTick", at = @At("RETURN"))
    private void celeritas$endFrame(boolean advanceGameTime, CallbackInfo ci) {
        this.celeritas$renderAheadManager.endFrame();
    }
}
