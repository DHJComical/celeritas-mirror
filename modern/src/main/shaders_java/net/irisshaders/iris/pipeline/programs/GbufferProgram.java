package net.irisshaders.iris.pipeline.programs;

import com.mojang.blaze3d.platform.GlStateManager;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.gl.blending.BlendModeOverride;
import net.irisshaders.iris.gl.blending.BufferBlendOverride;
import net.irisshaders.iris.gl.framebuffer.GlFramebuffer;
import net.irisshaders.iris.gl.program.Program;
import net.irisshaders.iris.gl.program.ProgramBuilder;
import net.irisshaders.iris.gl.program.ProgramSamplers;
import net.irisshaders.iris.gl.program.ProgramUniforms;
import net.irisshaders.iris.gl.texture.TextureType;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.samplers.IrisSamplers;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.irisshaders.iris.uniforms.custom.CustomUniforms;
import net.irisshaders.iris.vertices.ImmediateState;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.ARBTextureSwizzle;
import org.lwjgl.opengl.GL30C;
//? if <26.1 {
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL20C;
//?} else {
/*import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.opengl.GlBuffer;
import com.mojang.blaze3d.opengl.GlProgram;
import com.mojang.blaze3d.opengl.Uniform;
import net.irisshaders.iris.pipeline.foss_transform.VanillaUniformBlocks;
import org.lwjgl.opengl.GL31C;
*///?}

import java.util.List;

/**
 * A shader pack program for something vanilla draws, such as entities, particles or the sky. Before 26.1, vanilla
 * draws with an {@code IrisShaderInstance}, which binds the current pipeline's program for its {@link ShaderKey}. On
 * 26.1, the program is bound in place of vanilla's once vanilla has set up a draw.
 */
public final class GbufferProgram {
	private static final int[] INTENSITY_SWIZZLE = {GL30C.GL_RED, GL30C.GL_RED, GL30C.GL_RED, GL30C.GL_RED};

	private final Program program;
	@Nullable
	private final CustomUniforms customUniforms;
	private final IrisRenderingPipeline parent;
	private final GlFramebuffer writingToBeforeTranslucent;
	private final GlFramebuffer writingToAfterTranslucent;
	@Nullable
	private final BlendModeOverride blendModeOverride;
	private final List<BufferBlendOverride> bufferBlendOverrides;
	private final float alphaTest;
	private final boolean intensitySwizzle;
	private final boolean usesTessellation;

	//? if <26.1 {
	private final int modelViewLocation;
	private final int modelViewInverseLocation;
	private final int normalMatrixLocation;
	private final int projectionLocation;
	private final int projectionInverseLocation;
	private final int textureMatrixLocation;
	private final int chunkOffsetLocation;
	private final int colorModulatorLocation;
	private final int glintAlphaLocation;
	private final int fogStartLocation;
	private final int fogEndLocation;
	private final int fogColorLocation;
	private final int light0DirectionLocation;
	private final int light1DirectionLocation;

	private final Matrix4f scratchMatrix = new Matrix4f();
	private final Matrix3f scratchNormalMatrix = new Matrix3f();
	private final float[] scratchMatrix4 = new float[16];
	private final float[] scratchMatrix3 = new float[9];
	//?} else {
	/*private static final VanillaUniformBlocks[] BLOCKS = VanillaUniformBlocks.values();
	// Where blocks a vanilla program doesn't declare are bound, past the bindings vanilla assigns its programs.
	private static final int FIRST_SPARE_BLOCK_BINDING = 16;

	// By VanillaUniformBlocks ordinal, or -1 if the program doesn't use the block.
	private final int[] blockIndices = new int[BLOCKS.length];
	private final boolean[] blockIsSpare = new boolean[BLOCKS.length];
	@Nullable
	private GlProgram blockBindingsFor;
	*///?}

	/**
	 * Builds the program from a builder whose uniforms and samplers have been added. Custom uniforms, if given, must
	 * already be assigned to the builder.
	 */
	GbufferProgram(ProgramBuilder builder, @Nullable CustomUniforms customUniforms, IrisRenderingPipeline parent,
				   GlFramebuffer writingToBeforeTranslucent, GlFramebuffer writingToAfterTranslucent,
				   @Nullable BlendModeOverride blendModeOverride, List<BufferBlendOverride> bufferBlendOverrides,
				   float alphaTest, boolean intensitySwizzle, boolean usesTessellation) {
		if (customUniforms != null) {
			customUniforms.mapholderToPass(builder, this);
		}

		this.program = builder.build();
		this.customUniforms = customUniforms;
		this.parent = parent;
		this.writingToBeforeTranslucent = writingToBeforeTranslucent;
		this.writingToAfterTranslucent = writingToAfterTranslucent;
		this.blendModeOverride = blendModeOverride;
		this.bufferBlendOverrides = bufferBlendOverrides;
		this.alphaTest = alphaTest;
		this.intensitySwizzle = intensitySwizzle;
		this.usesTessellation = usesTessellation;

		int id = program.handle();
		//? if <26.1 {
		this.modelViewLocation = GlStateManager._glGetUniformLocation(id, "iris_ModelViewMat");
		this.modelViewInverseLocation = GlStateManager._glGetUniformLocation(id, "iris_ModelViewMatInverse");
		this.normalMatrixLocation = GlStateManager._glGetUniformLocation(id, "iris_NormalMat");
		this.projectionLocation = GlStateManager._glGetUniformLocation(id, "iris_ProjMat");
		this.projectionInverseLocation = GlStateManager._glGetUniformLocation(id, "iris_ProjMatInverse");
		this.textureMatrixLocation = GlStateManager._glGetUniformLocation(id, "iris_TextureMat");
		this.chunkOffsetLocation = GlStateManager._glGetUniformLocation(id, "iris_ChunkOffset");
		this.colorModulatorLocation = GlStateManager._glGetUniformLocation(id, "iris_ColorModulator");
		this.glintAlphaLocation = GlStateManager._glGetUniformLocation(id, "iris_GlintAlpha");
		this.fogStartLocation = GlStateManager._glGetUniformLocation(id, "iris_FogStart");
		this.fogEndLocation = GlStateManager._glGetUniformLocation(id, "iris_FogEnd");
		this.fogColorLocation = GlStateManager._glGetUniformLocation(id, "iris_FogColor");
		this.light0DirectionLocation = GlStateManager._glGetUniformLocation(id, "iris_Light0_Direction");
		this.light1DirectionLocation = GlStateManager._glGetUniformLocation(id, "iris_Light1_Direction");
		//?} else {
		/*for (VanillaUniformBlocks block : BLOCKS) {
			int index = IrisRenderSystem.getUniformBlockIndex(id, block.getBlockName());
			blockIndices[block.ordinal()] = index == GL31C.GL_INVALID_INDEX ? -1 : index;
		}
		*///?}
	}

	public void bind(VanillaDrawState state) {
		CapturedRenderingState.INSTANCE.setCurrentAlphaTest(alphaTest);

		if (intensitySwizzle) {
			IrisRenderSystem.texParameteriv(state.albedoTexture, TextureType.TEXTURE_2D.getGlType(), ARBTextureSwizzle.GL_TEXTURE_SWIZZLE_RGBA, INTENSITY_SWIZZLE);
		}

		IrisRenderSystem.bindTextureToUnit(TextureType.TEXTURE_2D.getGlType(), IrisSamplers.ALBEDO_TEXTURE_UNIT, state.albedoTexture);
		IrisRenderSystem.bindTextureToUnit(TextureType.TEXTURE_2D.getGlType(), IrisSamplers.OVERLAY_TEXTURE_UNIT, state.overlayTexture);
		IrisRenderSystem.bindTextureToUnit(TextureType.TEXTURE_2D.getGlType(), IrisSamplers.LIGHTMAP_TEXTURE_UNIT, state.lightmapTexture);
		//? if >=26.1 {
		/*// Filtering and wrapping are sampler state on 26.1, not texture state.
		IrisRenderSystem.bindSamplerToUnit(IrisSamplers.ALBEDO_TEXTURE_UNIT, state.albedoSampler);
		IrisRenderSystem.bindSamplerToUnit(IrisSamplers.OVERLAY_TEXTURE_UNIT, state.overlaySampler);
		IrisRenderSystem.bindSamplerToUnit(IrisSamplers.LIGHTMAP_TEXTURE_UNIT, state.lightmapSampler);
		CapturedRenderingState.INSTANCE.setCurrentAtlasSize(state.atlasWidth, state.atlasHeight);

		if (state.cloudFacesTexture != 0) {
			GlStateManager._activeTexture(GL31C.GL_TEXTURE0 + IrisSamplers.CLOUD_FACES_TEXTURE_UNIT);
			GL31C.glBindTexture(GL31C.GL_TEXTURE_BUFFER, state.cloudFacesTexture);
		}
		*///?}

		ImmediateState.usingTessellation = usesTessellation;

		program.bind();
		//? if <26.1 {
		uploadVanillaUniforms(state);
		//?} else
		//bindVanillaBlocks(state);

		if (customUniforms != null) {
			customUniforms.push(this);
		}

		if (blendModeOverride != null) {
			blendModeOverride.apply();
		}

		bufferBlendOverrides.forEach(BufferBlendOverride::apply);

		if (parent.isBeforeTranslucent) {
			writingToBeforeTranslucent.bind();
		} else {
			writingToAfterTranslucent.bind();
		}
	}

	public void unbind() {
		ProgramUniforms.clearActiveUniforms();
		ProgramSamplers.clearActiveSamplers();

		if (blendModeOverride != null || !bufferBlendOverrides.isEmpty()) {
			BlendModeOverride.restore();
		}
	}

	//? if <26.1 {
	private void uploadVanillaUniforms(VanillaDrawState state) {
		uploadMatrix(modelViewLocation, state.modelView);
		uploadMatrix(projectionLocation, state.projection);
		uploadMatrix(textureMatrixLocation, state.textureMatrix);

		if (modelViewInverseLocation != -1) {
			uploadMatrix(modelViewInverseLocation, scratchMatrix.set(state.modelView).invert());
		}

		if (projectionInverseLocation != -1) {
			uploadMatrix(projectionInverseLocation, scratchMatrix.set(state.projection).invert());
		}

		if (normalMatrixLocation != -1) {
			GL20C.glUniformMatrix3fv(normalMatrixLocation, false, scratchNormalMatrix.set(state.modelView).invert().transpose().get(scratchMatrix3));
		}

		uploadVector(chunkOffsetLocation, state.chunkOffset);
		uploadVector(light0DirectionLocation, state.light0Direction);
		uploadVector(light1DirectionLocation, state.light1Direction);
		uploadVector(colorModulatorLocation, state.colorModulator);
		uploadVector(fogColorLocation, state.fogColor);
		uploadFloat(glintAlphaLocation, state.glintAlpha);
		uploadFloat(fogStartLocation, state.fogStart);
		uploadFloat(fogEndLocation, state.fogEnd);
	}

	private void uploadMatrix(int location, Matrix4f matrix) {
		if (location != -1) {
			GL20C.glUniformMatrix4fv(location, false, matrix.get(scratchMatrix4));
		}
	}

	private static void uploadVector(int location, Vector3f vector) {
		if (location != -1) {
			IrisRenderSystem.uniform3f(location, vector.x, vector.y, vector.z);
		}
	}

	private static void uploadVector(int location, Vector4f vector) {
		if (location != -1) {
			IrisRenderSystem.uniform4f(location, vector.x, vector.y, vector.z, vector.w);
		}
	}

	private static void uploadFloat(int location, float value) {
		if (location != -1) {
			IrisRenderSystem.uniform1f(location, value);
		}
	}
	//?} else {
	/*/^*
	 * Points the program's blocks at the bindings vanilla's program reads them from, so that vanilla's buffers,
	 * including those it rebinds between draws, reach the program. Blocks vanilla's program lacks are bound to spare
	 * bindings, from the render pass if it has them.
	 ^/
	private void bindVanillaBlocks(VanillaDrawState state) {
		if (blockBindingsFor != state.vanillaProgram) {
			blockBindingsFor = state.vanillaProgram;
			int handle = program.handle();

			for (VanillaUniformBlocks block : BLOCKS) {
				int index = blockIndices[block.ordinal()];

				if (index == -1) {
					continue;
				}

				int binding = state.vanillaProgram.getUniform(block.getVanillaName()) instanceof Uniform.Ubo(int vanillaBinding) ? vanillaBinding : -1;
				blockIsSpare[block.ordinal()] = binding == -1;
				IrisRenderSystem.uniformBlockBinding(handle, index, binding == -1 ? FIRST_SPARE_BLOCK_BINDING + block.ordinal() : binding);
			}
		}

		for (VanillaUniformBlocks block : BLOCKS) {
			if (blockIndices[block.ordinal()] != -1 && blockIsSpare[block.ordinal()]) {
				GpuBufferSlice slice = state.uniforms.get(block.getVanillaName());

				if (slice != null) {
					GL31C.glBindBufferRange(GL31C.GL_UNIFORM_BUFFER, FIRST_SPARE_BLOCK_BINDING + block.ordinal(),
						((GlBuffer) slice.buffer()).handle, slice.offset(), slice.length());
				}
			}
		}
	}
	*///?}

	public boolean hasActiveImages() {
		return program.getActiveImages() > 0;
	}

	public void destroy() {
		program.destroy();
	}
}
