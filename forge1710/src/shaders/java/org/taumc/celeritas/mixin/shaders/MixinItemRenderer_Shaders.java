package org.taumc.celeritas.mixin.shaders;

import net.irisshaders.iris.pathways.HandRenderer;
import net.minecraft.client.renderer.ItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.taumc.celeritas.api.v0.CeleritasShadersApi;
import org.taumc.celeritas.compat.InteractionHand;

@Mixin(ItemRenderer.class)
public class MixinItemRenderer_Shaders {
    @Inject(method = "renderItemInFirstPerson", at = @At("HEAD"), cancellable = true)
    private void iris$skipTranslucentHands(float partialTicks, CallbackInfo ci) {
        if (CeleritasShadersApi.getInstance().isShaderPackInUse()) {
            boolean isHandTranslucent = HandRenderer.INSTANCE.isHandTranslucent(InteractionHand.MAIN_HAND);
            if (HandRenderer.INSTANCE.isRenderingSolid() && isHandTranslucent) {
                ci.cancel();
            } else if (!HandRenderer.INSTANCE.isRenderingSolid() && !isHandTranslucent) {
                ci.cancel();
            }
        }
    }
}
