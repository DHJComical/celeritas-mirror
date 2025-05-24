package org.taumc.celeritas.mixin.shaders.statelisteners;

import net.minecraft.client.renderer.ActiveRenderInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.taumc.celeritas.state.MatrixRenderingState;

import java.nio.FloatBuffer;

@Mixin(ActiveRenderInfo.class)
public class MixinActiveRenderInfo_MatrixState {
    @Shadow private static FloatBuffer modelview;
    @Shadow private static FloatBuffer projection;

    @Inject(method = "updateRenderInfo", at = @At(value = "TAIL"))
    private static void angelica$onUpdateRenderInfo(CallbackInfo ci) {
        MatrixRenderingState.INSTANCE.setProjectionMatrix(projection);
        MatrixRenderingState.INSTANCE.setModelViewMatrix(modelview);
    }
}
