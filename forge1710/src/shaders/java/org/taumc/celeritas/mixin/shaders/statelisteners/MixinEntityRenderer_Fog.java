package org.taumc.celeritas.mixin.shaders.statelisteners;

import net.irisshaders.iris.gl.state.StateUpdateNotifiers;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.minecraft.client.renderer.EntityRenderer;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class MixinEntityRenderer_Fog {
    @Shadow
    public float fogColorRed, fogColorGreen, fogColorBlue;

    private static Runnable fogStartListener;
    private static Runnable fogEndListener;

    static {
        StateUpdateNotifiers.fogStartNotifier = listener -> fogStartListener = listener;
        StateUpdateNotifiers.fogEndNotifier = listener -> fogEndListener = listener;
    }

    @Redirect(method="setupFog", at=@At(value="INVOKE", target ="Lorg/lwjgl/opengl/GL11;glFogf(IF)V"))
    void shader$fogListener(int pname, float param) {
        if(pname == GL11.GL_FOG_START) {
            if (fogStartListener != null) {
                fogStartListener.run();
            }
        } else if (pname == GL11.GL_FOG_END) {
            if (fogEndListener != null) {
                fogEndListener.run();
            }
        }
        GL11.glFogf(pname, param);
    }

    @Inject(method="updateFogColor", at=@At(value = "RETURN"))
    void shader$fogColor(float partialTicks, CallbackInfo ci) {
        CapturedRenderingState.INSTANCE.setFogColor(fogColorRed, fogColorGreen, fogColorBlue);
    }
}
