package org.embeddedt.embeddium.impl.mixin.core.render.world;

import net.minecraft.client.renderer.SectionBufferBuilderPool;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(SectionBufferBuilderPool.class)
public class SectionBufferBuilderPoolMixin {
    // NonStoringBuilderPool passes no buffers, and ArrayBlockingQueue rejects a capacity of zero.
    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Queues;newArrayBlockingQueue(I)Ljava/util/concurrent/ArrayBlockingQueue;", remap = false))
    private static int celeritas$allowEmptyPool(int capacity) {
        return Math.max(1, capacity);
    }
}
