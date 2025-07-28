package org.embeddedt.embeddium.impl.mixin.core.render.blaze;

//? if neoforge && >=1.21.5 {

/*import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.neoforged.neoforge.client.ClientHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientHooks.class)
public class B3DClientHooksMixin {
    /^*
     * @author embeddedt
     * @reason the validation wrappers prevent easily casting to GL-specific implementations, which we rely on for
     * code cleanliness (we do not support alternate backends)
     ^/
    @ModifyExpressionValue(method = "createGpuDevice", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/common/ModConfigSpec$BooleanValue;getAsBoolean()Z"))
    private static boolean disableValidation(boolean original) {
        return false;
    }
}
*///?}
