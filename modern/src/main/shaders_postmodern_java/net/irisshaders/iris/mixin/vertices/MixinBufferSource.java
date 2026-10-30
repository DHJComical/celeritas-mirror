package net.irisshaders.iris.mixin.vertices;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.irisshaders.iris.vertices.ImmediateState;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Extends the vertex format of what buffer sources build for the level.
 */
@Mixin(MultiBufferSource.BufferSource.class)
public class MixinBufferSource {
	@WrapOperation(method = "getBuffer", at = @At(value = "NEW", target = "(Lcom/mojang/blaze3d/vertex/ByteBufferBuilder;Lcom/mojang/blaze3d/vertex/VertexFormat$Mode;Lcom/mojang/blaze3d/vertex/VertexFormat;)Lcom/mojang/blaze3d/vertex/BufferBuilder;"))
	private BufferBuilder iris$extendLevelBuilders(ByteBufferBuilder buffer, VertexFormat.Mode mode, VertexFormat format, Operation<BufferBuilder> original) {
		ImmediateState.extendBufferSourceBuilders = ImmediateState.isRenderingLevel;

		try {
			return original.call(buffer, mode, format);
		} finally {
			ImmediateState.extendBufferSourceBuilders = false;
		}
	}
}
