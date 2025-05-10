package net.irisshaders.iris.compat.sodium.impl.shader_overrides;

import java.util.List;

import static com.mitchej123.glsm.GLStateManagerService.GL_STATE_MANAGER;
import static com.mitchej123.glsm.RenderSystemService.RENDER_SYSTEM;

import net.irisshaders.iris.Iris;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.gl.blending.BlendModeOverride;
import net.irisshaders.iris.gl.blending.BufferBlendOverride;
import net.irisshaders.iris.gl.framebuffer.GlFramebuffer;
import net.irisshaders.iris.gl.program.ProgramImages;
import net.irisshaders.iris.gl.program.ProgramSamplers;
import net.irisshaders.iris.gl.program.ProgramUniforms;
import net.irisshaders.iris.gl.texture.TextureType;
import net.irisshaders.iris.pipeline.SodiumTerrainPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.samplers.IrisSamplers;
import net.irisshaders.iris.shadows.ShadowRenderingState;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.irisshaders.iris.uniforms.custom.CustomUniforms;
import net.irisshaders.iris.vertices.ImmediateState;
import net.minecraft.client.Minecraft;
import org.embeddedt.embeddium.impl.gl.buffer.GlMutableBuffer;
import org.embeddedt.embeddium.impl.gl.shader.ShaderBindingContext;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformBlock;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformFloat3v;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformMatrix3f;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformMatrix4f;
import org.embeddedt.embeddium.impl.gl.tessellation.GlPrimitiveType;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderInterface;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderOptions;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.util.TextureUtil;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.opengl.GL32C;

public class IrisChunkShaderInterface implements ChunkShaderInterface {
	@Nullable
	private final GlUniformMatrix4f uniformModelViewMatrix;
	@Nullable
	private final GlUniformMatrix4f uniformModelViewMatrixInverse;
	@Nullable
	private final GlUniformMatrix4f uniformProjectionMatrix;
	@Nullable
	private final GlUniformMatrix4f uniformProjectionMatrixInverse;
	@Nullable
	private final GlUniformFloat3v uniformRegionOffset;
	@Nullable
	private final GlUniformMatrix3f uniformNormalMatrix;
	@Nullable
	private final GlUniformBlock uniformBlockDrawParameters;

	private final BlendModeOverride blendModeOverride;
	private final IrisShaderFogComponent fogShaderComponent;
	private final float alpha;
	private final ProgramUniforms irisProgramUniforms;
	private final ProgramSamplers irisProgramSamplers;
	private final ProgramImages irisProgramImages;
	private final List<BufferBlendOverride> bufferBlendOverrides;
	private final boolean hasOverrides;
	private final boolean isTess;
	private final CustomUniforms customUniforms;

	public IrisChunkShaderInterface(int handle, ShaderBindingContext contextExt, SodiumTerrainPipeline pipeline, ChunkShaderOptions options,
									boolean isTess, boolean isShadowPass, BlendModeOverride blendModeOverride, List<BufferBlendOverride> bufferOverrides, float alpha, CustomUniforms customUniforms) {
		this.uniformModelViewMatrix = contextExt.bindUniformIfPresent("iris_ModelViewMatrix", GlUniformMatrix4f::new);
		this.uniformModelViewMatrixInverse = contextExt.bindUniformIfPresent("iris_ModelViewMatrixInverse", GlUniformMatrix4f::new);
		this.uniformProjectionMatrix = contextExt.bindUniformIfPresent("iris_ProjectionMatrix", GlUniformMatrix4f::new);
		this.uniformProjectionMatrixInverse = contextExt.bindUniformIfPresent("iris_ProjectionMatrixInverse", GlUniformMatrix4f::new);
		this.uniformRegionOffset = contextExt.bindUniformIfPresent("u_RegionOffset", GlUniformFloat3v::new);
		this.uniformNormalMatrix = contextExt.bindUniformIfPresent("iris_NormalMatrix", GlUniformMatrix3f::new);
		this.uniformBlockDrawParameters = contextExt.bindUniformBlockIfPresent("ubo_DrawParameters", 0);
		this.customUniforms = customUniforms;
		this.isTess = isTess;

		this.alpha = alpha;

		this.blendModeOverride = blendModeOverride;
		this.bufferBlendOverrides = bufferOverrides;
		this.hasOverrides = bufferBlendOverrides != null && !bufferBlendOverrides.isEmpty();
		this.fogShaderComponent = new IrisShaderFogComponent(contextExt);

		ProgramUniforms.Builder builder = pipeline.initUniforms(handle);
		customUniforms.mapholderToPass(builder, this);
		this.irisProgramUniforms = builder.buildUniforms();
		this.irisProgramSamplers
			= isShadowPass ? pipeline.initShadowSamplers(handle) : pipeline.initTerrainSamplers(handle);
		this.irisProgramImages = isShadowPass ? pipeline.initShadowImages(handle) : pipeline.initTerrainImages(handle);
	}

	@Override
	public void setupState(TerrainRenderPass pass) {
        bindFramebuffer(pass);

        if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
            // No back face culling during the shadow pass
            // TODO: Hopefully this won't be necessary in the future...
            RENDER_SYSTEM.disableCullFace();
        }

		// See IrisSamplers#addLevelSamplers
		IrisRenderSystem.bindTextureToUnit(TextureType.TEXTURE_2D.getGlType(), IrisSamplers.ALBEDO_TEXTURE_UNIT, TextureUtil.getBlockTextureId());
		IrisRenderSystem.bindTextureToUnit(TextureType.TEXTURE_2D.getGlType(), IrisSamplers.LIGHTMAP_TEXTURE_UNIT, TextureUtil.getLightTextureId());
		// This is what is expected by the rest of rendering state, failure to do this will cause blurry textures on particles.
		GL_STATE_MANAGER.glActiveTexture(GL32C.GL_TEXTURE0 + IrisSamplers.LIGHTMAP_TEXTURE_UNIT);
		GL_STATE_MANAGER.glActiveTexture(GL32C.GL_TEXTURE0 + IrisSamplers.LIGHTMAP_TEXTURE_UNIT);
		CapturedRenderingState.INSTANCE.setCurrentAlphaTest(alpha);

		if (blendModeOverride != null) {
			blendModeOverride.apply();
		}

		ImmediateState.usingTessellation = isTess;

		if (hasOverrides) {
			bufferBlendOverrides.forEach(BufferBlendOverride::apply);
		}

		fogShaderComponent.setup();
		irisProgramUniforms.update();
		irisProgramSamplers.update();
		irisProgramImages.update();

		customUniforms.push(this);
	}

	public void restoreState() {
        unbindFramebuffer();

		ImmediateState.usingTessellation = false;

		if (blendModeOverride != null || hasOverrides) {
			BlendModeOverride.restore();
		}
	}

    @Override
    public GlPrimitiveType getPrimitiveType() {
        return isTess ? GlPrimitiveType.PATCHES : ChunkShaderInterface.super.getPrimitiveType();
    }

    @Override
	public void setProjectionMatrix(Matrix4fc matrix) {
		if (this.uniformProjectionMatrix != null) {
			this.uniformProjectionMatrix.set(matrix);
		}

		if (this.uniformProjectionMatrixInverse != null) {
			Matrix4f inverted = new Matrix4f(matrix);
			inverted.invert();
			this.uniformProjectionMatrixInverse.set(inverted);
		}
	}

	@Override
	public void setModelViewMatrix(Matrix4fc modelView) {
		if (this.uniformModelViewMatrix != null) {
			this.uniformModelViewMatrix.set(modelView);
		}

		if (this.uniformModelViewMatrixInverse != null) {
			Matrix4f invertedMatrix = new Matrix4f(modelView);
			invertedMatrix.invert();
			this.uniformModelViewMatrixInverse.set(invertedMatrix);
			if (this.uniformNormalMatrix != null) {
				invertedMatrix.transpose();
				this.uniformNormalMatrix.set(new Matrix3f(invertedMatrix));
			}
		} else if (this.uniformNormalMatrix != null) {
			Matrix3f normalMatrix = new Matrix3f(modelView);
			normalMatrix.invert();
			normalMatrix.transpose();
			this.uniformNormalMatrix.set(normalMatrix);
		}
	}

	public void setDrawUniforms(GlMutableBuffer buffer) {
		if (this.uniformBlockDrawParameters != null) {
			this.uniformBlockDrawParameters.bindBuffer(buffer);
		}
	}

	@Override
	public void setRegionOffset(float x, float y, float z) {
		if (this.uniformRegionOffset != null) {
			this.uniformRegionOffset.set(x, y, z);
		}
	}

    private SodiumTerrainPipeline getSodiumTerrainPipeline() {
        WorldRenderingPipeline worldRenderingPipeline = Iris.getPipelineManager().getPipelineNullable();

        if (worldRenderingPipeline != null) {
            return worldRenderingPipeline.getSodiumTerrainPipeline();
        } else {
            return null;
        }
    }

    public void bindFramebuffer(TerrainRenderPass pass) {
        SodiumTerrainPipeline pipeline = getSodiumTerrainPipeline();
        boolean isShadowPass = ShadowRenderingState.areShadowsCurrentlyBeingRendered();

        if (pipeline != null) {
            GlFramebuffer framebuffer;

            if (isShadowPass) {
                framebuffer = pipeline.getShadowFramebuffer();
            } else if (pass.isReverseOrder()) {
                framebuffer = pipeline.getTranslucentFramebuffer();
            } else {
                framebuffer = pipeline.getTerrainSolidFramebuffer();
            }

            if (framebuffer != null) {
                framebuffer.bind();
            }
        }
    }

    public void unbindFramebuffer() {
        SodiumTerrainPipeline pipeline = getSodiumTerrainPipeline();

        if (pipeline != null) {
            Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
        }
    }
}
