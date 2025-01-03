package net.irisshaders.iris.compat.sodium.mixin.shader_overrides;

import org.embeddedt.embeddium.impl.gl.shader.GlProgram;
import org.embeddedt.embeddium.impl.render.chunk.DefaultChunkRenderer;
import net.irisshaders.iris.compat.sodium.impl.shader_overrides.ShaderChunkRendererExt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(DefaultChunkRenderer.class)
public abstract class MixinRegionChunkRenderer implements ShaderChunkRendererExt {
	@Redirect(method = "render",
		at = @At(value = "INVOKE",
			target = "org/embeddedt/embeddium/impl/gl/shader/GlProgram.getInterface ()Ljava/lang/Object;"))
	private Object iris$getInterface(GlProgram<?> program) {
		if (program == null) {
			// Iris sentinel null
			return iris$getOverride().getInterface();
		} else {
			return program.getInterface();
		}
	}

}
