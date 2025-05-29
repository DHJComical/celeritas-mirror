package net.irisshaders.iris.pipeline;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableSet;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.irisshaders.iris.gl.blending.AlphaTest;
import net.irisshaders.iris.gl.blending.BlendModeOverride;
import net.irisshaders.iris.gl.framebuffer.GlFramebuffer;
import net.irisshaders.iris.gl.state.FogMode;
import net.irisshaders.iris.gl.state.ShaderAttributeInputs;
import net.irisshaders.iris.gl.state.ShaderAttributeInputsBuilder;
import net.irisshaders.iris.pathways.HorizonRenderer;
import net.irisshaders.iris.pipeline.programs.*;
import net.irisshaders.iris.shaderpack.loading.ProgramId;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shaderpack.properties.PackDirectives;
import net.irisshaders.iris.shadows.CommonShadowRenderer;
import net.irisshaders.iris.shadows.ShadowCompositeRenderer;
import net.irisshaders.iris.shadows.ModernShadowRenderer;
import net.irisshaders.iris.shadows.ShadowRenderTargets;
import net.irisshaders.iris.targets.*;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.irisshaders.iris.uniforms.custom.CustomUniforms;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.embeddedt.embeddium.compat.mc.*;
import org.jetbrains.annotations.Nullable;

public class ModernIrisRenderingPipeline extends CommonIrisRenderingPipeline implements IrisRenderingPipeline, WorldRenderingPipeline, ShaderRenderingPipeline, RenderTargetStateListener {
    private final HorizonRenderer horizonRenderer = new HorizonRenderer();
	private final int stackSize = 0;


	@Override
	protected void renderHorizon() {
		// TODO: Pull HorizonRenderer into common-shaders and pull this up
		horizonRenderer.renderHorizon(
				CapturedRenderingState.INSTANCE.getGbufferModelView(),
				CapturedRenderingState.INSTANCE.getGbufferProjection(),
				GameRenderer.getPositionShader());
	}

	@Override
    protected void updateMCFBInfo() {
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();

        this.mainFBHeight = main.height;
        this.mainFBWidth = main.width;
        this.mainFBDepthTextureId = main.getDepthTextureId();
        this.mainFBDepthBufferVersion = ((Blaze3dRenderTargetExt) main).iris$getDepthBufferVersion();
    }

    public ModernIrisRenderingPipeline(ProgramSet programSet) {
        super(programSet);
	}

	@Override
	protected boolean checkShadowUsesImages() {
		MCShaderInstance shader = getShaderMap().getShader(ModernShaderKey.SHADOW_TERRAIN_CUTOUT);
		boolean shadowUsesImages = false;

		if (shader instanceof ExtendedShader shader2) {
			shadowUsesImages = shader2.hasActiveImages();
		}

		return shadowUsesImages;
	}

	@Override
	protected void destroyHorizonRenderer() {
		horizonRenderer.destroy();
	}

	@Override
	protected @Nullable CommonShadowRenderer createShadowRenderer(CommonIrisRenderingPipeline commonIrisRenderingPipeline, ProgramSource programSource,
			PackDirectives packDirectives, ShadowRenderTargets shadowRenderTargets, ShadowCompositeRenderer shadowCompositeRenderer,
			CustomUniforms customUniforms, boolean separateHardwareSamplers) {
		return new ModernShadowRenderer(commonIrisRenderingPipeline, programSource, packDirectives, shadowRenderTargets, shadowCompositeRenderer, customUniforms, separateHardwareSamplers);
	}

	@Override
	protected ShaderKey[] getShaderKeyValues() {
		return ModernShaderKey.values();
	}

	@Override
	protected CompletableFuture<MCShaderInstance> createShader(String name, Executor syncExecutor, ProgramSource source, ProgramId programId, AlphaTest fallbackAlpha,
										MCVertexFormat vertexFormat, FogMode fogMode,
										boolean isIntensity, boolean isFullbright, boolean isGlint, boolean isText) throws IOException {
		GlFramebuffer beforeTranslucent = renderTargets.createGbufferFramebuffer(getFlippedAfterPrepare(), source.getDirectives().getDrawBuffers());
		GlFramebuffer afterTranslucent = renderTargets.createGbufferFramebuffer(getFlippedAfterTranslucent(), source.getDirectives().getDrawBuffers());
		boolean isLines = programId == ProgramId.Line && resolver.has(ProgramId.Line);


		ShaderAttributeInputs inputs = new ShaderAttributeInputsBuilder(vertexFormat, isFullbright, isLines, isGlint, isText).build();

		Supplier<ImmutableSet<Integer>> flipped =
			() -> isBeforeTranslucent ? getFlippedAfterPrepare() : getFlippedAfterTranslucent();


        CompletableFuture<ExtendedShader> extendedShaderFuture = ShaderCreator.create(this, syncExecutor, name, source, programId, beforeTranslucent, afterTranslucent,
			fallbackAlpha, vertexFormat, inputs,
				getFrameUpdateNotifier(), this, flipped, fogMode, isIntensity, isFullbright, false, isLines,
				getCustomUniforms());

        return extendedShaderFuture.thenApplyAsync(shader -> {
            loadedShaders.add(shader);
            return shader;
        }, syncExecutor);
	}

	@Override
	protected MCShaderInstance createFallbackShader(String name, ShaderKey key) throws IOException {
		GlFramebuffer beforeTranslucent = renderTargets.createGbufferFramebuffer(getFlippedAfterPrepare(), new int[]{0});
		GlFramebuffer afterTranslucent = renderTargets.createGbufferFramebuffer(getFlippedAfterTranslucent(), new int[]{0});

		FallbackShader shader = ShaderCreator.createFallback(name, beforeTranslucent, afterTranslucent,
			key.getAlphaTest(), key.getVertexFormat(), null, this, key.getFogMode(),
			key == ModernShaderKey.GLINT, key.isText(), key.hasDiffuseLighting(), key.isIntensity(), key.shouldIgnoreLightmap());

		loadedShaders.add(shader);

		return shader;
	}

	@Override
	protected MCShaderInstance createFallbackShadowShader(String name, ShaderKey key) throws IOException {
		GlFramebuffer framebuffer = shadowRenderTargets.createShadowFramebuffer(ImmutableSet.of(), new int[]{0});

		FallbackShader shader = ShaderCreator.createFallback(name, framebuffer, framebuffer,
			key.getAlphaTest(), key.getVertexFormat(), BlendModeOverride.OFF, this, key.getFogMode(),
			key == ModernShaderKey.GLINT, key.isText(), key.hasDiffuseLighting(), key.isIntensity(), key.shouldIgnoreLightmap());

		loadedShaders.add(shader);

		return shader;
	}

	@Override
	protected CompletableFuture<MCShaderInstance> createShadowShader(String name, Executor syncExecutor, ProgramSource source, ProgramId programId, AlphaTest fallbackAlpha,
											  MCVertexFormat vertexFormat, boolean isIntensity, boolean isFullbright, boolean isText) throws IOException {
		GlFramebuffer framebuffer = shadowRenderTargets.createShadowFramebuffer(ImmutableSet.of(), source.getDirectives().hasUnknownDrawBuffers() ? new int[]{0, 1} : source.getDirectives().getDrawBuffers());
		boolean isLines = programId == ProgramId.Line && resolver.has(ProgramId.Line);

		ShaderAttributeInputs inputs = new ShaderAttributeInputsBuilder(vertexFormat, isFullbright, isLines, false, isText).build();

		Supplier<ImmutableSet<Integer>> flipped = () -> getFlippedBeforeShadow();

		CompletableFuture<ExtendedShader> extendedShaderFuture = ShaderCreator.create(this, syncExecutor, name, source, programId, framebuffer, framebuffer,
			fallbackAlpha, vertexFormat, inputs,
				getFrameUpdateNotifier(), this, flipped, FogMode.PER_VERTEX, isIntensity, isFullbright, true, isLines, getCustomUniforms());

        return extendedShaderFuture.thenApplyAsync(shader -> {
            loadedShaders.add(shader);
            return shader;
        }, syncExecutor);
	}

	@Override
	public RenderTargetStateListener getRenderTargetStateListener() {
		return this;
	}

	@Override
	public void addDebugText(List<String> messages) {
		if (this.shadowRenderer != null) {
			messages.add("");
			shadowRenderer.addDebugText(messages);
		} else {
			messages.add("");
			messages.add("[Iris] Shadow Maps: not used by shader pack");
		}
	}

}
