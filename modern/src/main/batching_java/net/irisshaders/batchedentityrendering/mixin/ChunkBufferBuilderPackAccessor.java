package net.irisshaders.batchedentityrendering.mixin;

import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(net.minecraft.client.renderer. /*? if <1.20.2 {*/ ChunkBufferBuilderPack /*?} else {*/ /*SectionBufferBuilderPack *//*?}*/ .class)
public interface ChunkBufferBuilderPackAccessor {
    //? if <1.21 {
    @Accessor
    java.util.Map<RenderType, com.mojang.blaze3d.vertex.BufferBuilder> getBuilders();
    //?} else {
    /*@Accessor("buffers")
    java.util.Map<RenderType, com.mojang.blaze3d.vertex.ByteBufferBuilder> getBuilders();
    *///?}
}
