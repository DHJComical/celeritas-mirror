package org.taumc.celeritas.mixin.features.options;

import net.minecraftforge.client.GuiIngameForge;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.taumc.celeritas.CeleritasVintage;

@Mixin(GuiIngameForge.class)
public class MixinGuiIngameForge {
    @Redirect(method = "renderGameOverlay", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;isFancyGraphicsEnabled()Z"))
    private boolean celeritas$redirectVignette() {
        return CeleritasVintage.options().quality.enableVignette;
    }
}