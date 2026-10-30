package org.embeddedt.embeddium.impl.mixin.core;

import org.embeddedt.embeddium.impl.Celeritas;
import net.minecraft.client.Minecraft;
import net.minecraft.util.profiling.ProfilerFiller;
import org.embeddedt.embeddium.impl.render.frame.RenderAheadManager;
import org.embeddedt.embeddium.impl.util.ProfilerUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftClientMixin {
    @Unique
    private final RenderAheadManager celeritas$renderAheadManager = new RenderAheadManager();

    /**
     * We run this at the beginning of the frame (except for the first frame) to give the previous frame plenty of time
     * to render on the GPU. This allows us to stall on ClientWaitSync for less time.
     */
    @Inject(method = "runTick", at = @At("HEAD"))
    private void preRender(boolean tick, CallbackInfo ci) {
        ProfilerFiller profiler = ProfilerUtil.get();
        profiler.push("wait_for_gpu");

        this.celeritas$renderAheadManager.startFrame(Celeritas.options().advanced.cpuRenderAheadLimit);

        profiler.pop();
    }

    @Inject(method = "runTick", at = @At("RETURN"))
    private void postRender(boolean tick, CallbackInfo ci) {
        this.celeritas$renderAheadManager.endFrame();
    }
}
