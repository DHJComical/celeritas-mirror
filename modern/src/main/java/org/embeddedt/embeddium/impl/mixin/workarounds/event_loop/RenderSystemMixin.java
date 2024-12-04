package org.embeddedt.embeddium.impl.mixin.workarounds.event_loop;

import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderSystem.class)
public class RenderSystemMixin {
    @Redirect(method = "flipFrame", at = @At(value = "INVOKE", target =
            //? if >=1.20 {
            "Lcom/mojang/blaze3d/systems/RenderSystem;pollEvents()V"
            //?} else
            /*"Lorg/lwjgl/glfw/GLFW;glfwPollEvents()V"*/
            , ordinal = 0)/*? if fabric || (forge && >=1.20) {*/, remap = false/*?}*/)
    private static void removeFirstPoll() {
        // noop
        // should fix some bugs with minecraft polling events twice for some reason (why does it do that in the first place?)
    }
}
