package org.taumc.celeritas.mixin.core;

import net.minecraft.client.Minecraft;
import org.embeddedt.embeddium.impl.render.frame.RenderAheadManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.taumc.celeritas.CeleritasVintage;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Unique
    private final RenderAheadManager celeritas$renderAheadManager = new RenderAheadManager();

    @Inject(method = "runTick", at = @At("HEAD"))
    private void preRender(CallbackInfo ci) {
        celeritas$renderAheadManager.startFrame(CeleritasVintage.options().advanced.cpuRenderAheadLimit);
    }

    @Inject(method = "runTick", at = @At("RETURN"))
    private void postRender(CallbackInfo ci) {
        celeritas$renderAheadManager.endFrame();
    }
}
