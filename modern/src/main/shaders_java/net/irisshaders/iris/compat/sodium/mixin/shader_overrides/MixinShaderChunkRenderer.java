package net.irisshaders.iris.compat.sodium.mixin.shader_overrides;

import org.embeddedt.embeddium.impl.gl.device.RenderDevice;
import org.embeddedt.embeddium.impl.gl.shader.GlProgram;
import org.embeddedt.embeddium.impl.render.CeleritasWorldRenderer;
import org.embeddedt.embeddium.impl.render.chunk.RenderPassConfiguration;
import org.embeddedt.embeddium.impl.render.chunk.ShaderChunkRenderer;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderInterface;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderOptions;
import net.irisshaders.iris.compat.sodium.impl.shader_overrides.IrisChunkProgramOverrides;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Overrides shaders in {@link ShaderChunkRenderer} with our own as needed.
 */
@Mixin(ShaderChunkRenderer.class)
public class MixinShaderChunkRenderer {
	@Unique
	private IrisChunkProgramOverrides irisChunkProgramOverrides;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void iris$onInit(RenderDevice device, RenderPassConfiguration<?> renderPassConfiguration, CallbackInfo ci) {
		irisChunkProgramOverrides = new IrisChunkProgramOverrides();
	}

    @Inject(method = "compileProgram", at = @At("HEAD"), cancellable = true)
	private void iris$useOverride(ChunkShaderOptions options, CallbackInfoReturnable<GlProgram<ChunkShaderInterface>> cir) {
		RenderPassConfiguration<?> configuration = CeleritasWorldRenderer.instance().getRenderPassConfiguration();
		var override = irisChunkProgramOverrides.getProgramOverride(options.pass(), configuration);

        if (override != null) {
            cir.setReturnValue((GlProgram<ChunkShaderInterface>)(GlProgram<?>)override);
        }
	}

	@Inject(method = "delete", at = @At("HEAD"))
	private void iris$onDelete(CallbackInfo ci) {
		irisChunkProgramOverrides.deleteShaders();
	}
}
