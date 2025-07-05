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
    /**
     * @author embeddedt
     * @reason apparently b7.3 uses the default depth buffer precision (8-bit), which looks very bad
     */
    @Redirect(method = "init", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/Display;create()V"))
    private void createWithHighPrecisionDepthBuffer() throws LWJGLException {
        Display.create(new PixelFormat().withDepthBits(24));
    }

    /**
     * @author embeddedt
     * @reason by default b7.3 does not mark the window as resizeable
     */
    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/Display;setTitle(Ljava/lang/String;)V"))
    private void makeResizeable(CallbackInfo ci) {
        Display.setResizable(true);
    }

    @ModifyArg(method = "init", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/Display;setTitle(Ljava/lang/String;)V"), index = 0)
    private String removeDuplicateMinecraftInTitle(String newTitle) {
        if (newTitle.startsWith("Minecraft Minecraft")) {
            return newTitle.replaceFirst("^Minecraft ", "");
        } else {
            return newTitle;
        }
    }
}
