package org.embeddedt.embeddium.impl.mixin.features.options.overlays;

import org.embeddedt.embeddium.impl.SodiumClientMod;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(VanillaGuiOverlay.class)
public class VanillaGuiOverlayMixin {

    @Redirect(method = "lambda$static$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;useFancyGraphics()Z"))
    private static boolean redirectFancyGraphicsVignette() {
        return SodiumClientMod.options().quality.enableVignette;
    }

}
