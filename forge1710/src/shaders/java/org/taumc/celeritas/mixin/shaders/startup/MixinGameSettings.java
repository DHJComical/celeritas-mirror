package org.taumc.celeritas.mixin.shaders.startup;

import net.irisshaders.iris.IrisCommon;
import net.minecraft.client.settings.GameSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameSettings.class)
public class MixinGameSettings {
    @Unique
    private static boolean iris$initialized;

    @Inject(method="Lnet/minecraft/client/settings/GameSettings;loadOptions()V", at=@At("HEAD"))
    private void celeritas$InitializeShaders(CallbackInfo ci) {
        if (iris$initialized) {
            return;
        }

        iris$initialized = true;
        IrisCommon.onEarlyInitialize();
    }

}
