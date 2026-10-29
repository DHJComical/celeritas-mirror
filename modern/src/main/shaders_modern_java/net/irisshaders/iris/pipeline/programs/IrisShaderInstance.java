package net.irisshaders.iris.pipeline.programs;

import com.mojang.blaze3d.shaders.ProgramManager;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pipeline.ShaderRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
//? if >=1.20.6 {
/*import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.repository.PackSource;
*///?}
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.embeddedt.embeddium.impl.util.PlatformUtil;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * What vanilla draws with in place of its own shader for a {@link ShaderKey}. Vanilla sets the uniforms and calls
 * {@link #apply()} as it would for any shader, which binds the current pipeline's {@link GbufferProgram} for the key.
 * The GL program vanilla creates for the instance is a placeholder that is never drawn with.
 * <p>
 * There is one instance per key for the whole session, independent of which pipeline is active.
 */
public final class IrisShaderInstance extends ShaderInstance {
	private static final Map<ShaderKey, IrisShaderInstance> INSTANCES = new EnumMap<>(ShaderKey.class);

	private final ShaderKey key;
	private final VanillaDrawState drawState = new VanillaDrawState();
	// No initializer, since the superclass constructor fills it through getUniform before field initializers run.
	private Map<String, Uniform> uniformValues;
	@Nullable
	private GbufferProgram bound;

	private IrisShaderInstance(ShaderKey key) throws IOException {
		super(PlaceholderResources.INSTANCE, key.getName(), key.getVertexFormat());
		this.key = key;
	}

	public static IrisShaderInstance get(ShaderKey key) {
		RenderSystem.assertOnRenderThread();
		return INSTANCES.computeIfAbsent(key, IrisShaderInstance::create);
	}

	private static IrisShaderInstance create(ShaderKey key) {
		try {
			return new IrisShaderInstance(key);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void apply() {
		bound = findProgram();

		if (bound == null) {
			// The pipeline this instance was handed out for is gone, so there is nothing to draw with.
			ProgramManager.glUseProgram(0);
			return;
		}

		drawState.modelView.set(0, MODEL_VIEW_MATRIX.getFloatBuffer());
		drawState.projection.set(0, PROJECTION_MATRIX.getFloatBuffer());
		drawState.textureMatrix.set(0, TEXTURE_MATRIX.getFloatBuffer());
		drawState.chunkOffset.set(0, CHUNK_OFFSET.getFloatBuffer());
		drawState.colorModulator.set(0, COLOR_MODULATOR.getFloatBuffer());
		drawState.fogColor.set(0, FOG_COLOR.getFloatBuffer());
		drawState.light0Direction.set(0, LIGHT0_DIRECTION.getFloatBuffer());
		drawState.light1Direction.set(0, LIGHT1_DIRECTION.getFloatBuffer());
		drawState.glintAlpha = GLINT_ALPHA.getFloatBuffer().get(0);
		drawState.fogStart = FOG_START.getFloatBuffer().get(0);
		drawState.fogEnd = FOG_END.getFloatBuffer().get(0);
		drawState.albedoTexture = RenderSystem.getShaderTexture(0);
		drawState.overlayTexture = RenderSystem.getShaderTexture(1);
		drawState.lightmapTexture = RenderSystem.getShaderTexture(2);

		bound.bind(drawState);
	}

	@Override
	public void clear() {
		if (bound != null) {
			bound.unbind();
			bound = null;
		}

		Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
	}

	@Nullable
	private GbufferProgram findProgram() {
		WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();

		if (pipeline instanceof ShaderRenderingPipeline shaderPipeline) {
			return shaderPipeline.getShaderMap().getProgram(key);
		}

		return null;
	}

	/**
	 * Returns the uniforms vanilla writes to, which hold values for {@link #apply()} to read rather than belonging to
	 * the placeholder program. Any other uniform doesn't exist.
	 */
	@Nullable
	@Override
	public Uniform getUniform(String name) {
		if (uniformValues == null) {
			uniformValues = createUniformValues();
		}

		return uniformValues.get(name);
	}

	private Map<String, Uniform> createUniformValues() {
		float[] identity = new Matrix4f().get(new float[16]);
		Map<String, Uniform> values = new HashMap<>();

		addUniformValue(values, "ModelViewMat", Uniform.UT_MAT4, identity);
		addUniformValue(values, "ProjMat", Uniform.UT_MAT4, identity);
		addUniformValue(values, "TextureMat", Uniform.UT_MAT4, identity);
		addUniformValue(values, "ChunkOffset", Uniform.UT_FLOAT3, 0.0F, 0.0F, 0.0F);
		addUniformValue(values, "ColorModulator", Uniform.UT_FLOAT4, 1.0F, 1.0F, 1.0F, 1.0F);
		addUniformValue(values, "FogColor", Uniform.UT_FLOAT4, 0.0F, 0.0F, 0.0F, 0.0F);
		addUniformValue(values, "Light0_Direction", Uniform.UT_FLOAT3, 0.0F, 0.0F, 0.0F);
		addUniformValue(values, "Light1_Direction", Uniform.UT_FLOAT3, 0.0F, 0.0F, 0.0F);
		addUniformValue(values, "GlintAlpha", Uniform.UT_FLOAT1, 1.0F);
		addUniformValue(values, "FogStart", Uniform.UT_FLOAT1, 0.0F);
		addUniformValue(values, "FogEnd", Uniform.UT_FLOAT1, 1.0F);

		return values;
	}

	private void addUniformValue(Map<String, Uniform> values, String name, int type, float... initial) {
		Uniform uniform = new Uniform(name, type, initial.length, this);
		uniform.set(initial);
		values.put(name, uniform);
	}

	/**
	 * Provides the placeholder program, under whichever name vanilla asks for its definition.
	 */
	private static final class PlaceholderResources implements ResourceProvider {
		private static final PlaceholderResources INSTANCE = new PlaceholderResources();

		private static final String DEFINITION = """
			{ "vertex": "iris_placeholder", "fragment": "iris_placeholder" }
			""";
		private static final String VERTEX = """
			#version 150
			void main() { gl_Position = vec4(0.0); }
			""";
		private static final String FRAGMENT = """
			#version 150
			out vec4 fragColor;
			void main() { fragColor = vec4(0.0); }
			""";

		@Override
		public Optional<Resource> getResource(ResourceLocation id) {
			String path = id.getPath();

			if (path.endsWith(".json")) {
				return Optional.of(resource(DEFINITION));
			} else if (path.endsWith(".vsh")) {
				return Optional.of(resource(VERTEX));
			} else if (path.endsWith(".fsh")) {
				return Optional.of(resource(FRAGMENT));
			}

			return Optional.empty();
		}

		private static Resource resource(String content) {
			return new Resource(fabricateResourcePack(), () -> new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
		}

		private static PackResources fabricateResourcePack() {
			//? if >=1.20.6 {
			//return new PathPackResources(new PackLocationInfo("<iris placeholder shader>", Component.literal("iris"), PackSource.BUILT_IN, Optional.of(new KnownPack("iris", "shader", "1.0"))), PlatformUtil.getConfigDir());
			//?} else
			return new PathPackResources("<iris placeholder shader>", PlatformUtil.getConfigDir(), true);
		}
	}
}
