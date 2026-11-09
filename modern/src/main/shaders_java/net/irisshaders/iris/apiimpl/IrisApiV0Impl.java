package net.irisshaders.iris.apiimpl;

import net.irisshaders.iris.Iris;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.api.v0.IrisApiConfig;
import net.irisshaders.iris.api.v0.IrisTextVertexSink;
import net.irisshaders.iris.gui.screen.ShaderPackScreen;
import net.irisshaders.iris.pipeline.VanillaRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.shadows.ShadowRenderingState;
import net.irisshaders.iris.vertices.IrisTextVertexSinkImpl;
import net.minecraft.client.gui.screens.Screen;
//? if >=26.1 {
/*import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.irisshaders.iris.api.v0.IrisProgram;
import net.irisshaders.iris.api.v0.IrisShadowProgram;
import net.irisshaders.iris.api.v0.IrisShadowRenderCallback;
import net.irisshaders.iris.shadows.ShadowRenderCallbacks;
*///?}

import java.nio.ByteBuffer;
import java.util.function.IntFunction;

public class IrisApiV0Impl implements IrisApi {
	public static final IrisApiV0Impl INSTANCE = new IrisApiV0Impl();
	private static final IrisApiV0ConfigImpl CONFIG = new IrisApiV0ConfigImpl();

	@Override
	public int getMinorApiRevision() {
		//? if >=26.1 {
		/*return 4;
		*///?} else
		return 2;
	}

	@Override
	public boolean isShaderPackInUse() {
		WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();

		if (pipeline == null) {
			return false;
		}

		return !(pipeline instanceof VanillaRenderingPipeline);
	}

	@Override
	public boolean isRenderingShadowPass() {
		return ShadowRenderingState.areShadowsCurrentlyBeingRendered();
	}

	@Override
	public Object openMainIrisScreenObj(Object parent) {
		return new ShaderPackScreen((Screen) parent);
	}

	@Override
	public String getMainScreenLanguageKey() {
		return "options.iris.shaderPackSelection";
	}

	@Override
	public IrisApiConfig getConfig() {
		return CONFIG;
	}

	@Override
	public IrisTextVertexSink createTextVertexSink(int maxQuadCount, IntFunction<ByteBuffer> bufferProvider) {
		return new IrisTextVertexSinkImpl(maxQuadCount, bufferProvider);
	}

	@Override
	public float getSunPathRotation() {
		WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();

		if (pipeline == null) {
			return 0;
		}

		return pipeline.getSunPathRotation();
	}
	//? if >=26.1 {

	/*@Override
	public void assignPipeline(RenderPipeline pipeline, IrisProgram program) {
		IrisApiPipelines.assign(pipeline, program);
	}

	@Override
	public void assignPipelineShadow(RenderPipeline pipeline, IrisShadowProgram program) {
		IrisApiPipelines.assignShadow(pipeline, program);
	}

	@Override
	public void registerShadowRenderCallback(IrisShadowRenderCallback callback) {
		ShadowRenderCallbacks.register(callback);
	}
	*///?}
}
