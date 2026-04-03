package org.embeddedt.embeddium.impl.mixin.features.options.overlays;

import org.embeddedt.embeddium.impl.Celeritas;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Gui.class)
public class InGameHudMixin {
    @Redirect(method = /*? if <1.20.6 {*/ "render" /*?} else {*/ /*"renderCameraOverlays" *//*?}*/, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;useFancyGraphics()Z"))
    private boolean redirectFancyGraphicsVignette() {
        return Celeritas.options().quality.enableVignette;
    }
}
