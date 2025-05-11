package org.taumc.celeritas.mixin.core;

import net.minecraft.client.Minecraft;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.PixelFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    /**
     * @author embeddedt
     * @reason apparently b7.3 uses the default depth (8-bit), which looks very bad
     */
    @Redirect(method = "init", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/Display;create()V"))
    private void createWithHighPrecisionDepthBuffer() throws LWJGLException {
        Display.create(new PixelFormat().withDepthBits(24));
    }
}
