package net.irisshaders.iris.compat.sodium.mixin.vertex_format;

import org.embeddedt.embeddium.impl.render.CeleritasWorldRenderer;
import org.embeddedt.embeddium.impl.render.chunk.RenderSectionManager;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexType;
import net.irisshaders.iris.compat.sodium.impl.vertex_format.IrisModelVertexFormats;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(CeleritasWorldRenderer.class)
public class MixinRenderSectionManager {
	@ModifyArg(method = "initRenderer", index = 0, remap = false,
		at = @At(value = "INVOKE",
			target = "Lorg/embeddedt/embeddium/impl/modern/render/chunk/ModernRenderSectionManager;create(Lorg/embeddedt/embeddium/impl/render/chunk/vertex/format/ChunkVertexType;Lnet/minecraft/client/multiplayer/ClientLevel;ILorg/embeddedt/embeddium/impl/gl/device/CommandList;)Lorg/embeddedt/embeddium/impl/modern/render/chunk/ModernRenderSectionManager;"))
	private ChunkVertexType iris$useExtendedVertexFormat$1(ChunkVertexType vertexType) {
		return WorldRenderingSettings.INSTANCE.shouldUseExtendedVertexFormat() ? IrisModelVertexFormats.MODEL_VERTEX_XHFP : vertexType;
	}
}
