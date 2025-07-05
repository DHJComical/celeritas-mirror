package org.taumc.celeritas.mixin.shaders.startup;

import net.irisshaders.iris.IrisCommon;
import net.minecraft.client.renderer.OpenGlHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OpenGlHelper.class)
public class MixinInitRenderer {
    @Inject(method = "initializeTextures", at = @At("RETURN"))
    private static void angelica$initializeRenderer(CallbackInfo ci) {
        IrisCommon.onRenderSystemInit();
    }
}
