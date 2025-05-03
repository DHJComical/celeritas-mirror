package org.taumc.celeritas.mixin.shaders.startup;

import net.irisshaders.iris.IrisCommon;
import net.minecraft.client.gui.GuiMainMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiMainMenu.class)
public class MixinGuiMainMenu {

    @Inject(method = "initGui", at = @At("RETURN"))
    public void angelica$shadersOnLoadingComplete(CallbackInfo ci) {
        IrisCommon.onLoadingComplete();
    }

}
