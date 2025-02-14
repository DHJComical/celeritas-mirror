package org.embeddedt.embeddium.impl.mixin.core;

import com.mojang.blaze3d.systems.RenderSystem;
import org.embeddedt.embeddium.impl.gl.debug.GLDebug;
import org.embeddedt.embeddium.impl.util.PlatformUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderSystem.class)
public class RenderSystemMixin {
    @Inject(method = "initRenderer", at = @At("RETURN"))
    private static void resetDebug(CallbackInfo ci) {
        if (PlatformUtil.isDevelopmentEnvironment()) {
            System.setProperty("celeritas.enableGLDebug", "true");
        }
        GLDebug.reloadDebugState();
    }
}
