package org.embeddedt.embeddium.impl.mixin.core.render.immediate.consumer;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ByteBufferBuilder.class)
public class ByteBufferBuilderMixin {
    @Shadow
    private long capacity;

    @Shadow
    @Final
    private long maxCapacity;

    @ModifyArg(method = "ensureCapacity", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/ByteBufferBuilder;resize(J)V"))
    private long embeddium$growGeometrically(long vanillaSize) {
        long geometric = this.capacity + (this.capacity >> 1);

        return Math.min(Math.max(vanillaSize, geometric), this.maxCapacity);
    }
}
