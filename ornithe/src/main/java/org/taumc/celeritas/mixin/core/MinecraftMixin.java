package org.taumc.celeritas.mixin.core;

import net.minecraft.client.Minecraft;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.PixelFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    private static final String INIT_DISPLAY_METHOD =
            //? if <1.8 {
            "init"
             //?} else
            /*"initDisplay"*/
    ;

    /**
     * @author embeddedt
     * @reason apparently b7.3 uses the default depth buffer precision (8-bit), which looks very bad
     */
    @Redirect(method = INIT_DISPLAY_METHOD, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/Display;create()V"))
    private void createWithHighPrecisionDepthBuffer() throws LWJGLException {
        Display.create(new PixelFormat().withDepthBits(24));
    }

    /**
     * @author embeddedt
     * @reason by default b7.3 does not mark the window as resizeable
     */
    @Inject(method = INIT_DISPLAY_METHOD, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/Display;setTitle(Ljava/lang/String;)V"))
    private void makeResizeable(CallbackInfo ci) {
        Display.setResizable(true);
    }

    @ModifyArg(method = INIT_DISPLAY_METHOD, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/Display;setTitle(Ljava/lang/String;)V"), index = 0)
    private String removeDuplicateMinecraftInTitle(String newTitle) {
        if (newTitle.startsWith("Minecraft Minecraft")) {
            return newTitle.replaceFirst("^Minecraft ", "");
        } else {
            return newTitle;
        }
    }
}
