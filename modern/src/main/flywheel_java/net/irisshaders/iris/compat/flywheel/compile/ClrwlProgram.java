package net.irisshaders.iris.compat.flywheel.compile;

import com.google.common.collect.ImmutableSet;
import net.irisshaders.iris.compat.flywheel.ClrwlSamplers;
import net.irisshaders.iris.compat.flywheel.engine.ClrwlBlendModeOverride;
import net.irisshaders.iris.compat.flywheel.engine.ClrwlInstanceVisual;
import net.irisshaders.iris.compat.flywheel.engine.ClrwlMaterialEncoder;
import net.irisshaders.iris.compat.flywheel.engine.ClrwlRenderingPhase;
import net.irisshaders.iris.compat.flywheel.engine.uniform.ClrwlUniforms;
import net.irisshaders.iris.compat.flywheel.shaderpack.ClrwlPackDirectives;
import net.irisshaders.iris.compat.flywheel.shaderpack.ClrwlProgramGroup;
import net.irisshaders.iris.compat.flywheel.shaderpack.ClrwlProgramId;
import net.irisshaders.iris.compat.flywheel.util.Utils;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.backend.engine.embed.EmbeddingUniforms;
import dev.engine_room.flywheel.backend.gl.shader.GlProgram;
import net.irisshaders.iris.gl.blending.BlendMode;
import net.irisshaders.iris.gl.program.ProgramImages;
import net.irisshaders.iris.gl.program.ProgramSamplers;
import net.irisshaders.iris.gl.program.ProgramUniforms;
import net.irisshaders.iris.gl.state.FogMode;
import net.irisshaders.iris.mixin.texture.TextureAtlasAccessor;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.uniforms.CommonUniforms;
import net.irisshaders.iris.uniforms.custom.CustomUniforms;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.lwjgl.opengl.GL20.glUniform2i;
import static org.lwjgl.opengl.GL20.glUniform4i;

public class ClrwlProgram extends GlProgram
{
	private final String name;
	private final ClrwlProgramId programId;

	private ProgramUniforms uniforms;
	private CustomUniforms customUniforms;
	private ProgramSamplers samplers;
	private ProgramImages images;

	public static ImmutableSet<Integer> getReservedTextureUnits(int coeffCount)
	{
		List<Integer> res = new ArrayList<>();

		res.add(ClrwlSamplers.DIFFUSE.number);
		res.add(ClrwlSamplers.OVERLAY.number);
		res.add(ClrwlSamplers.LIGHT.number);
		res.add(ClrwlSamplers.CRUMBLING.number);
		res.add(ClrwlSamplers.INSTANCE_BUFFER.number);
		res.add(ClrwlSamplers.LIGHT_LUT.number);
		res.add(ClrwlSamplers.LIGHT_SECTIONS.number);

		// OIT Samplers
		res.add(ClrwlSamplers.DEPTH_RANGE.number);
		res.add(ClrwlSamplers.NOISE.number);

		for (int i = 0; i < coeffCount; i++)
		{
			res.add(ClrwlSamplers.getCoefficient(i).number);
		}

		return ImmutableSet.copyOf(res);
	}

	public ClrwlProgram(String name, ClrwlProgramId programId, int handle)
	{
		super(handle);
		this.name = name;
		this.programId = programId;
	}

	public void preLink()
	{
		bindAttribLocation("_flw_aPos", 0);
		bindAttribLocation("_flw_aColor", 1);
		bindAttribLocation("_flw_aTexCoord", 2);
		bindAttribLocation("_flw_aLight", 3);
		bindAttribLocation("_flw_aNormal", 4);
		bindAttribLocation("_clrwl_aEntity", 5);
		bindAttribLocation("_clrwl_aMidTexCoord", 6);
		bindAttribLocation("_clrwl_aTangent", 7);
		bindAttribLocation("_clrwl_aMidBlock", 8);
		bindAttribLocation("_flw_aOverlay", 9);
	}

	public void postLink(IrisRenderingPipeline irisPipeline, ClrwlPackDirectives directives)
	{
		var handle = handle();
		var customUniforms = irisPipeline.getCustomUniforms();
		var oitConfig = directives.getOitConfig(programId.group());
		var oitRanks = oitConfig.coeffRanks();

		ProgramUniforms.Builder uniformBuilder = ProgramUniforms.builder(name, handle);
		ProgramSamplers.Builder samplerBuilder = ProgramSamplers.builder(handle, getReservedTextureUnits(oitRanks.length));
		ProgramImages.Builder   imageBuilder   = ProgramImages.builder(handle);

		samplerBuilder.addExternalSampler(ClrwlSamplers.DIFFUSE.number, "flw_diffuseTex");
		samplerBuilder.addExternalSampler(ClrwlSamplers.OVERLAY.number, "flw_overlayTex");
		// samplerBuilder.addExternalSampler(ClrwlSamplers.LIGHT.number, "flw_lightTex");
		samplerBuilder.addExternalSampler(ClrwlSamplers.CRUMBLING.number, "_flw_crumblingTex");
		samplerBuilder.addExternalSampler(ClrwlSamplers.INSTANCE_BUFFER.number, "_flw_instances");
		samplerBuilder.addExternalSampler(ClrwlSamplers.LIGHT_LUT.number, "_flw_lightLut");
		samplerBuilder.addExternalSampler(ClrwlSamplers.LIGHT_SECTIONS.number, "_flw_lightSections");
		samplerBuilder.addExternalSampler(ClrwlSamplers.DEPTH_RANGE.number, "_flw_depthRange");
		samplerBuilder.addExternalSampler(ClrwlSamplers.NOISE.number, "_flw_blueNoise");

		for (int i = 0; i < oitRanks.length; i++)
		{
			samplerBuilder.addExternalSampler(ClrwlSamplers.getCoefficient(i).number, "clrwl_coefficients" + i);
		}

		var isShadowPass = programId.group() == ClrwlProgramGroup.SHADOW;
		Supplier<ImmutableSet<Integer>> flipped = isShadowPass
				? irisPipeline::getFlippedBeforeShadow
				: programId.afterTranslucent()
				  ? irisPipeline::getFlippedAfterTranslucent
				  : irisPipeline::getFlippedAfterPrepare;

		CommonUniforms.addDynamicUniforms(uniformBuilder, FogMode.PER_VERTEX);
		customUniforms.assignTo(uniformBuilder);
		irisPipeline.addGbufferOrShadowSamplers(samplerBuilder, imageBuilder,
				flipped, isShadowPass,
				false, true, false); // Use Flywheel texture and overlay samplers
		customUniforms.mapholderToPass(uniformBuilder, this);

		this.uniforms = uniformBuilder.buildUniforms();
		this.customUniforms = customUniforms;
		this.samplers = samplerBuilder.build();
		this.images = imageBuilder.build();

		ClrwlUniforms.setUniformsBlockBindings(this);
	}

	public String name()
	{
		return name;
	}

	public void bind()
	{
		super.bind();

		samplers.update();
		uniforms.update();
		customUniforms.push(this);
		images.update();
	}

	public static void unbind()
	{
		ProgramUniforms.clearActiveUniforms();
		ProgramSamplers.clearActiveSamplers();
	}

	public void setClrwlCommonUniforms(Material material, ClrwlBlendModeOverride blendModeOverride, ClrwlRenderingPhase phase)
	{
		var abstractTexture = Minecraft.getInstance().getTextureManager().getTexture(material.texture());
		int atlasWidth = 0;
		int atlasHeight = 0;

		if (abstractTexture instanceof TextureAtlas atlas)
		{
			atlasWidth = ((TextureAtlasAccessor) atlas).callGetWidth();
			atlasHeight = ((TextureAtlasAccessor) atlas).callGetHeight();
		}

		BlendMode blendMode;

		if (blendModeOverride != null)
		{
			if (blendModeOverride.blendMode() != null)
			{
				blendMode = blendModeOverride.blendMode();
			}
			else
			{
				blendMode = Utils.transparencyToBlendMode(Transparency.OPAQUE);
			}
		}
		else
		{
			blendMode = Utils.transparencyToBlendMode(material.transparency());
		}

		setIVec4("_clrwl_blendFunc", blendMode.srcRgb(), blendMode.dstRgb(), blendMode.srcAlpha(), blendMode.dstAlpha());
		setIVec2("_clrwl_atlasSize", atlasWidth, atlasHeight);
		setInt("_clrwl_renderPhase", phase.getValue());
	}

	public void setInstancingUniforms(int baseVertex, int baseInstance, Material material, ClrwlInstanceVisual visual, Vector3fc meshCenter)
	{
		int packedMaterialProperties = ClrwlMaterialEncoder.packProperties(material);

		setUInt("_flw_baseVertex", baseVertex);
		setInt("_flw_baseInstance", baseInstance);
		setUInt("_clrwl_packedMaterialUniform", packedMaterialProperties);

		setInt("_clrwl_blockEntityIdUniform", visual.getBlockEntity());
		setInt("_clrwl_entityIdUniform", visual.getEntity());
		setVec4("_clrwl_meshCenterUniform", meshCenter.x(), meshCenter.y(), meshCenter.z(), (float) visual.lightEmission());
	}

	public void setBaseDrawUniform(int baseDraw)
	{
		setUInt("_flw_baseDraw", baseDraw);
	}

	public void setEmbeddedMatrices(Matrix4f model,  Matrix3f normal)
	{
		setMat4(EmbeddingUniforms.MODEL_MATRIX, model);
		setMat3(EmbeddingUniforms.NORMAL_MATRIX, normal);
	}

	public void free()
	{
		super.delete();
	}

	public void setIVec2(String glslName, int x, int y)
	{
		int uniform = getUniformLocation(glslName);

		if (uniform < 0) {
			return;
		}

		glUniform2i(uniform, x, y);
	}

	public void setIVec4(String glslName, int x, int y, int z, int w)
	{
		int uniform = getUniformLocation(glslName);

		if (uniform < 0) {
			return;
		}

		glUniform4i(uniform, x, y, z, w);
	}
}
