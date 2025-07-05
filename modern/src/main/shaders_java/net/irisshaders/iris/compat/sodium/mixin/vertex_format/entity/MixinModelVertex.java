package net.irisshaders.iris.compat.sodium.mixin.vertex_format.entity;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.mojang.blaze3d.vertex.PoseStack;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.compat.sodium.impl.vertex_format.entity_xhfp.EntityVertex;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.irisshaders.iris.vertices.ImmediateState;
import org.embeddedt.embeddium.api.util.NormI8;
import org.embeddedt.embeddium.api.vertex.buffer.VertexBufferWriter;
import org.embeddedt.embeddium.api.vertex.format.VertexFormatDescription;
import org.embeddedt.embeddium.api.vertex.format.common.ModelVertex;
import org.embeddedt.embeddium.impl.model.quad.ModelQuadView;
import org.embeddedt.embeddium.impl.render.immediate.model.BakedModelEncoder;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BakedModelEncoder.class)
public class MixinModelVertex {
	private static final int OFFSET_MID_TEXTURE = 42;

	/**
	 * @author IMS, embeddedt
	 * @reason Set the shared flag for whether vertex data is being extended.
	 */
	@Inject(method = "writeQuadVertices(Lorg/embeddedt/embeddium/api/vertex/buffer/VertexBufferWriter;Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lorg/embeddedt/embeddium/impl/model/quad/ModelQuadView;IIIZ)V", at = @At("HEAD"))
	private static void checkForExtension(VertexBufferWriter writer, PoseStack.Pose matrices, ModelQuadView quad, int color, int light, int overlay, boolean todo, CallbackInfo ci, @Share("shouldExtend") LocalBooleanRef shouldExtend) {
		shouldExtend.set(shouldBeExtended());
	}

	/**
	 * @author IMS, embeddedt
	 * @reason Increase the allocated buffer size if using the extended vertex format.
	 */
	@ModifyConstant(method = "writeQuadVertices(Lorg/embeddedt/embeddium/api/vertex/buffer/VertexBufferWriter;Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lorg/embeddedt/embeddium/impl/model/quad/ModelQuadView;IIIZ)V", constant = @Constant(intValue = 4 * ModelVertex.STRIDE))
	private static int getNewBufferSize(int prevSize, @Share("shouldExtend") LocalBooleanRef shouldExtend) {
		return shouldExtend.get() ? (4 * EntityVertex.STRIDE) : prevSize;
	}

	/**
	 * @author IMS, embeddedt
	 * @reason Increase the stride used for advancing the buffer pointer if using the extended vertex format.
	 */
	@ModifyConstant(method = "writeQuadVertices(Lorg/embeddedt/embeddium/api/vertex/buffer/VertexBufferWriter;Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lorg/embeddedt/embeddium/impl/model/quad/ModelQuadView;IIIZ)V", constant = @Constant(longValue = ModelVertex.STRIDE))
	private static long getNewStride(long prevSize, @Share("shouldExtend") LocalBooleanRef shouldExtend) {
		return shouldExtend.get() ? EntityVertex.STRIDE : prevSize;
	}

	/**
	 * @author IMS, embeddedt
	 * @reason Inject extended properties (mid U/V, captured rendering state) into the buffer.
	 */
	@Inject(method = "writeQuadVertices(Lorg/embeddedt/embeddium/api/vertex/buffer/VertexBufferWriter;Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lorg/embeddedt/embeddium/impl/model/quad/ModelQuadView;IIIZ)V", at = @At(value = "INVOKE", target = "Lorg/embeddedt/embeddium/api/vertex/format/common/ModelVertex;write(JFFFIFFIII)V"))
	private static void injectExtendedData(CallbackInfo ci, @Local(ordinal = 0, argsOnly = true) ModelQuadView quad, @Local(name = "ptr") long ptr, @Share("shouldExtend") LocalBooleanRef shouldExtend) {
		if (shouldExtend.get()) {
			writeExtendedData(quad, ptr);
		}
	}

	/**
	 * @author IMS, embeddedt
	 * @reason Do a second pass over the data and inject any extra properties (tangent, etc.)
	 */
	@Inject(method = "writeQuadVertices(Lorg/embeddedt/embeddium/api/vertex/buffer/VertexBufferWriter;Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lorg/embeddedt/embeddium/impl/model/quad/ModelQuadView;IIIZ)V", at = @At(value = "INVOKE", target = "Lorg/embeddedt/embeddium/api/vertex/buffer/VertexBufferWriter;push(Lorg/lwjgl/system/MemoryStack;JILorg/embeddedt/embeddium/api/vertex/format/VertexFormatDescription;)V"))
	private static void injectExtendedData(CallbackInfo ci, @Local(ordinal = 0, argsOnly = true) ModelQuadView quad, @Local(name = "ptr") long ptr, @Share("shouldExtend") LocalBooleanRef shouldExtend, @Local(name = "normal") int normal) {
		if (shouldExtend.get()) {
			endQuad(ptr, normal);
		}
	}

	/**
	 * @author IMS, embeddedt
	 * @reason Change the format used for pushing data if the extended format is used
	 */
	@ModifyArg(method = "writeQuadVertices(Lorg/embeddedt/embeddium/api/vertex/buffer/VertexBufferWriter;Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lorg/embeddedt/embeddium/impl/model/quad/ModelQuadView;IIIZ)V", at = @At(value = "INVOKE", target = "Lorg/embeddedt/embeddium/api/vertex/buffer/VertexBufferWriter;push(Lorg/lwjgl/system/MemoryStack;JILorg/embeddedt/embeddium/api/vertex/format/VertexFormatDescription;)V"), index = 3)
	private static VertexFormatDescription changePushFormat(VertexFormatDescription desc, @Share("shouldExtend") LocalBooleanRef shouldExtend) {
		return shouldExtend.get() ? EntityVertex.FORMAT : desc;
	}

	@Unique
	private static void endQuad(long ptr, int normal) {
		EntityVertex.endQuad(ptr - EntityVertex.STRIDE, NormI8.unpackX(normal), NormI8.unpackY(normal), NormI8.unpackZ(normal));
	}

	@Unique
	private static void writeExtendedData(ModelQuadView quad, long ptr) {
		float midU = ((quad.getTexU(0) + quad.getTexU(1) + quad.getTexU(2) + quad.getTexU(3)) * 0.25f);
		float midV = ((quad.getTexV(0) + quad.getTexV(1) + quad.getTexV(2) + quad.getTexV(3)) * 0.25f);

		MemoryUtil.memPutFloat(ptr + OFFSET_MID_TEXTURE, midU);
		MemoryUtil.memPutFloat(ptr + OFFSET_MID_TEXTURE + 4, midV);

		MemoryUtil.memPutShort(ptr + 36, (short) CapturedRenderingState.INSTANCE.getCurrentRenderedEntity());
		MemoryUtil.memPutShort(ptr + 38, (short) CapturedRenderingState.INSTANCE.getCurrentRenderedBlockEntity());
		MemoryUtil.memPutShort(ptr + 40, (short) CapturedRenderingState.INSTANCE.getCurrentRenderedItem());
	}

	// embeddedt - Previously the other writeQuadVertices method was overwritten here with a carbon copy of its contents, we remove this

	private static boolean shouldBeExtended() {
		return IrisApi.getInstance().isShaderPackInUse() && ImmediateState.renderWithExtendedVertexFormat;
	}
}