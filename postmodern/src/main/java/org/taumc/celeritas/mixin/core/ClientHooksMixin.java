package org.taumc.celeritas.mixin.core;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.neoforged.neoforge.client.ClientHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientHooks.class)
public class ClientHooksMixin {
    @ModifyExpressionValue(method = "createGpuDevice", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/common/ModConfigSpec$BooleanValue;getAsBoolean()Z"))
    private static boolean disableValidation(boolean original) {
        return false;
    }
}