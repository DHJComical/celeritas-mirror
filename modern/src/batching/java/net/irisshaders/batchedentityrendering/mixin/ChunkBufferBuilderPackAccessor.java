package net.irisshaders.batchedentityrendering.mixin;

import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(net.minecraft.client.renderer. /*? if <1.20.2 {*/ ChunkBufferBuilderPack /*?} else {*/ /*SectionBufferBuilderPack *//*?}*/ .class)
public interface ChunkBufferBuilderPackAccessor {
	@Accessor
	Map<RenderType, BufferBuilder> getBuilders();
}
