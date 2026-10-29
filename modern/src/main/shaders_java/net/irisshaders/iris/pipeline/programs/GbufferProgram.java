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
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.ARBTextureSwizzle;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;

import java.util.List;

/**
 * A shader pack program for something vanilla draws, such as entities, particles or the sky. Vanilla draws with an
 * {@link IrisShaderInstance}, which binds the current pipeline's program for its {@link ShaderKey}.
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
	}

	public void bind(VanillaDrawState state) {
		CapturedRenderingState.INSTANCE.setCurrentAlphaTest(alphaTest);

		if (intensitySwizzle) {
			IrisRenderSystem.texParameteriv(state.albedoTexture, TextureType.TEXTURE_2D.getGlType(), ARBTextureSwizzle.GL_TEXTURE_SWIZZLE_RGBA, INTENSITY_SWIZZLE);
		}

		IrisRenderSystem.bindTextureToUnit(TextureType.TEXTURE_2D.getGlType(), IrisSamplers.ALBEDO_TEXTURE_UNIT, state.albedoTexture);
		IrisRenderSystem.bindTextureToUnit(TextureType.TEXTURE_2D.getGlType(), IrisSamplers.OVERLAY_TEXTURE_UNIT, state.overlayTexture);
		IrisRenderSystem.bindTextureToUnit(TextureType.TEXTURE_2D.getGlType(), IrisSamplers.LIGHTMAP_TEXTURE_UNIT, state.lightmapTexture);

		ImmediateState.usingTessellation = usesTessellation;

		program.bind();
		uploadVanillaUniforms(state);

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

	public boolean hasActiveImages() {
		return program.getActiveImages() > 0;
	}

	public void destroy() {
		program.destroy();
	}
}
