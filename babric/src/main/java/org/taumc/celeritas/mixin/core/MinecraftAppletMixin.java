package org.taumc.celeritas.mixin.core;

import net.minecraft.client.MinecraftApplet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MinecraftApplet.class)
public class MinecraftAppletMixin {
    /**
     * @author embeddedt
     * @reason The client thread is not a daemon thread, so we should not need to wait for it to exit. This prevents
     * a deadlock between the AWT & client threads that causes a freeze when exiting the game.
     */
    @Redirect(method = "stopThread", at = @At(value = "INVOKE", target = "Ljava/lang/Thread;join(J)V"), require = 0)
    private void celeritas$skipJoin(Thread instance, long time) {

    }
}
