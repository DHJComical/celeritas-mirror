package net.irisshaders.batchedentityrendering.mixin;

import net.irisshaders.batchedentityrendering.impl.MemoryTrackingBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiBufferSource.BufferSource.class)
public class MixinBufferSource implements MemoryTrackingBuffer {
    //? if <1.21 {
	@Shadow
	@Final
	protected com.mojang.blaze3d.vertex.BufferBuilder builder;

	@Shadow
	@Final
	protected java.util.Map<RenderType, com.mojang.blaze3d.vertex.BufferBuilder> fixedBuffers;
    //?} else {
    /*@Shadow
    @Final
    protected com.mojang.blaze3d.vertex.ByteBufferBuilder sharedBuffer;

    @Shadow
    @Final
    protected java.util.SequencedMap<RenderType, com.mojang.blaze3d.vertex.ByteBufferBuilder> fixedBuffers;

    @Unique
    private com.mojang.blaze3d.vertex.ByteBufferBuilder builder;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void assignOurField(CallbackInfo ci) {
        this.builder = this.sharedBuffer;
    }
    *///?}

	@Override
	public long getAllocatedSize() {
        long allocatedSize = ((MemoryTrackingBuffer) builder).getAllocatedSize();

		for (var fixed : fixedBuffers.values()) {
			allocatedSize += ((MemoryTrackingBuffer) fixed).getAllocatedSize();
		}

		return allocatedSize;
	}

	@Override
	public long getUsedSize() {
        long allocatedSize = ((MemoryTrackingBuffer) builder).getUsedSize();

		for (var fixed : fixedBuffers.values()) {
			allocatedSize += ((MemoryTrackingBuffer) fixed).getUsedSize();
		}

		return allocatedSize;
	}

	@Override
	public void freeAndDeleteBuffer() {
		((MemoryTrackingBuffer) builder).freeAndDeleteBuffer();

		for (var fixed : fixedBuffers.values()) {
			((MemoryTrackingBuffer) fixed).freeAndDeleteBuffer();
		}
	}
}
