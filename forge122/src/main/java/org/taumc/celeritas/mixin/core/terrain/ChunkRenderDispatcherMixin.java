package org.taumc.celeritas.mixin.core.terrain;

import com.google.common.collect.Queues;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.ArrayBlockingQueue;

@Mixin(ChunkRenderDispatcher.class)
public class ChunkRenderDispatcherMixin {

    @Shadow
    @Final
    @Mutable
    private int countRenderBuilders;

    private boolean celeritas$isVanillaDispatcher() {
        return ((Object)this).getClass() == ChunkRenderDispatcher.class;
    }

    @Redirect(method = "<init>(I)V", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Queues;newArrayBlockingQueue(I)Ljava/util/concurrent/ArrayBlockingQueue;"))
    public ArrayBlockingQueue<?> modifyThreadPoolSize(int capacity) {
        if (celeritas$isVanillaDispatcher()) {
            // Do not allow any resources to be allocated
            this.countRenderBuilders = 0;
            capacity = 1;
        }
        return Queues.newArrayBlockingQueue(capacity);
    }

    /**
     * @author embeddedt
     * @reason mods expect there to be render builders
     */
    @ModifyReturnValue(method = "hasNoFreeRenderBuilders", at = @At("RETURN"))
    private boolean hasNoFreeRenderBuilders(boolean original) {
        return original && !celeritas$isVanillaDispatcher();
    }
}
