package net.irisshaders.iris.mixin.vertices;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.irisshaders.iris.vertices.ImmediateState;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Draws meshes built with an extended vertex format with that format, rather than their pipeline's.
 */
@Mixin(RenderType.class)
public abstract class MixinRenderType {
	@Shadow
	public abstract VertexFormat format();

	@WrapMethod(method = "draw")
	private void iris$drawWithMeshFormat(MeshData mesh, Operation<Void> original) {
		VertexFormat meshFormat = mesh.drawState().format();
		ImmediateState.meshFormatOverride = meshFormat != format() ? meshFormat : null;

		try {
			original.call(mesh);
		} finally {
			ImmediateState.meshFormatOverride = null;
		}
	}
}
