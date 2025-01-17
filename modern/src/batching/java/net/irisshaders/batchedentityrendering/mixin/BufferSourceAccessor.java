package net.irisshaders.batchedentityrendering.mixin;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MultiBufferSource.BufferSource.class)
public interface BufferSourceAccessor {
	@Accessor
    //? if <1.21 {
    java.util.Map<RenderType, com.mojang.blaze3d.vertex.BufferBuilder> getFixedBuffers();
    //?} else
    /*java.util.SequencedMap<RenderType, com.mojang.blaze3d.vertex.ByteBufferBuilder> getFixedBuffers();*/
}
