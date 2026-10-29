package net.irisshaders.iris.pipeline.programs;

import com.mojang.blaze3d.opengl.GlRenderPass;
import com.mojang.blaze3d.opengl.GlRenderPipeline;
import com.mojang.blaze3d.opengl.GlSampler;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.opengl.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.pipeline.IrisPipelines;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.texture.AtlasTextures;
import net.irisshaders.iris.vertices.ImmediateState;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

/**
 * Swaps the shader pack's gbuffer program in for vanilla's once vanilla has set up a draw in the world.
 */
public final class GbufferProgramBinder {
	private static final VanillaDrawState STATE = new VanillaDrawState();
	@Nullable
	private static GbufferProgram bound;

	private GbufferProgramBinder() {
	}

	public static void afterSetup(GlRenderPass renderPass) {
		if (!(Iris.getPipelineManager().getPipelineNullable() instanceof IrisRenderingPipeline pipeline)
			|| !pipeline.shouldOverrideShaders()) {
			unbind();
			return;
		}

		GlRenderPipeline compiled = renderPass.pipeline;
		ShaderKey key = IrisPipelines.getKey(pipeline, compiled.info());
		GbufferProgram program = key != null ? pipeline.getShaderMap().getProgram(key) : null;

		if (program == null) {
			unbind();
			pipeline.bindDefault();
			return;
		}

		if (bound != null && bound != program) {
			bound.unbind();
		}

		STATE.vanillaProgram = compiled.program();
		STATE.uniforms = renderPass.uniforms;
		STATE.cloudFacesTexture = compiled.program().getUniform("CloudFaces") instanceof Uniform.Utb cloudFaces ? cloudFaces.texture() : 0;

		GlRenderPass.TextureViewAndSampler albedo = renderPass.samplers.get("Sampler0");
		GlRenderPass.TextureViewAndSampler overlay = renderPass.samplers.get("Sampler1");
		GlRenderPass.TextureViewAndSampler lightmap = renderPass.samplers.get("Sampler2");

		if (albedo != null) {
			GlTexture texture = albedo.view().texture();
			boolean atlas = AtlasTextures.isAtlas(texture);
			STATE.albedoTexture = texture.glId();
			STATE.albedoSampler = albedo.sampler().getId();
			STATE.atlasWidth = atlas ? texture.getWidth(0) : 0;
			STATE.atlasHeight = atlas ? texture.getHeight(0) : 0;
		} else {
			STATE.albedoTexture = 0;
			STATE.albedoSampler = 0;
			STATE.atlasWidth = 0;
			STATE.atlasHeight = 0;
		}

		// Pack programs may sample the overlay and lightmap even where vanilla's program doesn't, so they fall back to
		// what vanilla binds them with elsewhere.
		Minecraft minecraft = Minecraft.getInstance();
		STATE.overlayTexture = overlay != null ? overlay.view().texture().glId() : glId(minecraft.gameRenderer.overlayTexture().getTextureView());
		STATE.overlaySampler = overlay != null ? overlay.sampler().getId() : linearClampSampler();
		STATE.lightmapTexture = lightmap != null ? lightmap.view().texture().glId() : glId(minecraft.gameRenderer.lightmap());
		STATE.lightmapSampler = lightmap != null ? lightmap.sampler().getId() : linearClampSampler();

		program.bind(STATE);
		bound = program;

		IrisRenderSystem.invalidateVanillaRenderState();
	}

	private static void unbind() {
		if (bound != null) {
			bound.unbind();
			bound = null;
		}

		ImmediateState.usingTessellation = false;
	}

	private static int glId(GpuTextureView view) {
		return ((GlTexture) view.texture()).glId();
	}

	private static int linearClampSampler() {
		return ((GlSampler) RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)).getId();
	}
}
