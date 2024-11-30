package org.embeddedt.embeddium.impl.mixin.features.options.overlays;

import org.embeddedt.embeddium.impl.Celeritas;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

//? if neoforge && <1.20.5 {
/*import net.neoforged.neoforge.client.gui.overlay.VanillaGuiOverlay;

@Mixin(VanillaGuiOverlay.class)
public class VanillaGuiOverlayMixin {

    @Redirect(method = "lambda$static$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;useFancyGraphics()Z"))
    private static boolean redirectFancyGraphicsVignette() {
        return Celeritas.options().quality.enableVignette;
    }

}
*///?} else if forge && <1.20.6 && >=1.19 {
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;

@Mixin(VanillaGuiOverlay.class)
public class VanillaGuiOverlayMixin {

    @Redirect(method = "lambda$static$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;useFancyGraphics()Z"))
    private static boolean redirectFancyGraphicsVignette() {
        return Celeritas.options().quality.enableVignette;
    }

}
//?} else if forge && <1.19 {
/*import net.minecraftforge.client.gui.ForgeIngameGui;

@Mixin(ForgeIngameGui.class)
public class VanillaGuiOverlayMixin {

    @Redirect(method = {
            //? if >=1.18
            "lambda$static$0"
            //? if <1.18
            /^"render"^/
    }, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;useFancyGraphics()Z"))
    private /^? if >=1.18 {^/ static /^?}^/ boolean redirectFancyGraphicsVignette() {
        return Celeritas.options().quality.enableVignette;
    }

}

*///?}