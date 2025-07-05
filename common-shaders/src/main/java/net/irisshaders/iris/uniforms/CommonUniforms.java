package net.irisshaders.iris.uniforms;

import static com.mitchej123.glsm.GLStateManagerService.GL_STATE_MANAGER;
import static net.irisshaders.iris.gl.uniform.UniformUpdateFrequency.*;
import static net.irisshaders.iris.uniforms.IdMapUniforms.ID_MAP_UNIFORMS;
import static org.embeddedt.embeddium.compat.mc.MinecraftVersionShimService.MINECRAFT_SHIM;

import net.irisshaders.iris.compat.dh.DHCompat;
import net.irisshaders.iris.gl.blending.BlendMode;
import net.irisshaders.iris.gl.state.FogMode;
import net.irisshaders.iris.gl.state.StateUpdateNotifiers;
import net.irisshaders.iris.gl.uniform.DynamicUniformHolder;
import net.irisshaders.iris.gl.uniform.UniformHolder;
import net.irisshaders.iris.layer.GbufferPrograms;
import net.irisshaders.iris.shaderpack.IdMap;
import net.irisshaders.iris.shaderpack.properties.PackDirectives;
import net.irisshaders.iris.uniforms.transforms.SmoothedFloat;
import net.irisshaders.iris.uniforms.transforms.SmoothedVec2f;
import org.joml.*;
import org.joml.Math;

public final class CommonUniforms {
	private static final Vector4i ZERO_VECTOR_4i = new Vector4i(0, 0, 0, 0);

	static {
		GbufferPrograms.init();
	}

	private CommonUniforms() {
		// no construction allowed
	}

	// Needs to use a LocationalUniformHolder as we need it for the common uniforms
	public static void addDynamicUniforms(DynamicUniformHolder uniforms, FogMode fogMode) {
		ExternallyManagedUniforms.addExternallyManagedUniforms117(uniforms);
		FogUniforms.addFogUniforms(uniforms, fogMode);
		IrisInternalUniforms.addFogUniforms(uniforms, fogMode);

		// This is a fallback for when entityId via attributes cannot be used. (lightning)
		uniforms.uniform1i( "entityId", CapturedRenderingState.INSTANCE::getCurrentRenderedEntity, StateUpdateNotifiers.fallbackEntityNotifier);

		// TODO: OptiFine doesn't think that atlasSize is a "dynamic" uniform,
		//       but we do. How will custom uniforms depending on atlasSize work?
		//
		// Note: on 1.17+ we don't need to reset this when textures are bound, since
		// the shader will always be setup (and therefore uniforms will be re-uploaded)
		// after the texture is changed and before rendering starts.
		uniforms.uniform2i("atlasSize", MINECRAFT_SHIM::getAtlasSize, listener -> {});

		uniforms.uniform2i("gtextureSize", MINECRAFT_SHIM::getTextureSize, StateUpdateNotifiers.bindTextureNotifier);

		uniforms.uniform4i("blendFunc", () -> {
            if (GL_STATE_MANAGER.isBlendEnabled()) {
                final BlendMode blend = GL_STATE_MANAGER.getBlendMode();
                return new Vector4i(blend.srcRgb(), blend.dstRgb(), blend.srcAlpha(), blend.dstAlpha());
            } else {
                return ZERO_VECTOR_4i;
            }
		}, StateUpdateNotifiers.blendFuncNotifier);

		uniforms.uniform1i("renderStage", () -> GbufferPrograms.getCurrentPhase().ordinal(), StateUpdateNotifiers.phaseChangeNotifier);
	}

	public static void addCommonUniforms(DynamicUniformHolder uniforms, IdMap idMap, PackDirectives directives, FrameUpdateNotifier updateNotifier, FogMode fogMode) {
		CommonUniforms.addNonDynamicUniforms(uniforms, idMap, directives, updateNotifier);
		CommonUniforms.addDynamicUniforms(uniforms, fogMode);
	}

	public static void addNonDynamicUniforms(UniformHolder uniforms, IdMap idMap, PackDirectives directives, FrameUpdateNotifier updateNotifier) {
		CameraUniforms.addCameraUniforms(uniforms, updateNotifier);
		ViewportUniforms.addViewportUniforms(uniforms);
		WorldTimeUniforms.addWorldTimeUniforms(uniforms);
		SystemTimeUniforms.addSystemTimeUniforms(uniforms);
		BiomeUniforms.BIOME_UNIFORMS.addBiomeUniforms(uniforms);
		new CelestialUniforms(directives.getSunPathRotation()).addCelestialUniforms(uniforms);
		IrisExclusiveUniforms.addIrisExclusiveUniforms(uniforms);
		IrisTimeUniforms.addTimeUniforms(uniforms);
		MatrixUniforms.addMatrixUniforms(uniforms, directives);
        ID_MAP_UNIFORMS.addIdMapUniforms(updateNotifier, uniforms, idMap, directives.isOldHandLight());
		CommonUniforms.generalCommonUniforms(uniforms, updateNotifier, directives);
	}

	public static void generalCommonUniforms(UniformHolder uniforms, FrameUpdateNotifier updateNotifier, PackDirectives directives) {
		ExternallyManagedUniforms.addExternallyManagedUniforms117(uniforms);

		SmoothedVec2f eyeBrightnessSmooth = new SmoothedVec2f(directives.getEyeBrightnessHalfLife(), directives.getEyeBrightnessHalfLife(), MINECRAFT_SHIM::getEyeBrightness, updateNotifier);

		uniforms
			.uniform1b(PER_FRAME, "hideGUI", MINECRAFT_SHIM::hideGui)
			.uniform1b(PER_FRAME, "isRightHanded", MINECRAFT_SHIM::isRightHanded)
			.uniform1i(PER_FRAME, "isEyeInWater", MINECRAFT_SHIM::isEyeInWater)
			.uniform1f(PER_FRAME, "blindness", MINECRAFT_SHIM::getBlindness)
			.uniform1f(PER_FRAME, "darknessFactor", MINECRAFT_SHIM::getDarknessFactor)
			.uniform1f(PER_FRAME, "darknessLightFactor", CapturedRenderingState.INSTANCE::getDarknessLightFactor)
			.uniform1f(PER_FRAME, "nightVision", MINECRAFT_SHIM::getNightVision)
			.uniform1b(PER_FRAME, "is_sneaking", MINECRAFT_SHIM::isSneaking)
			.uniform1b(PER_FRAME, "is_sprinting", MINECRAFT_SHIM::isSprinting)
			.uniform1b(PER_FRAME, "is_hurt", MINECRAFT_SHIM::isHurt)
			.uniform1b(PER_FRAME, "is_invisible", MINECRAFT_SHIM::isInvisible)
			.uniform1b(PER_FRAME, "is_burning", MINECRAFT_SHIM::isBurning)
			.uniform1b(PER_FRAME, "is_on_ground", MINECRAFT_SHIM::isOnGround)
			// TODO: Do we need to clamp this to avoid fullbright breaking shaders? Or should shaders be able to detect
			//       that the player is trying to turn on fullbright?
			.uniform1f(PER_FRAME, "screenBrightness", MINECRAFT_SHIM::getScreenBrightness)
			// just a dummy value for shaders where entityColor isn't supplied through a vertex attribute (and thus is
			// not available) - suppresses warnings. See AttributeShaderTransformer for the actual entityColor code.
			.uniform4f(ONCE, "entityColor", () -> new Vector4f(0, 0, 0, 0))
			.uniform1i(ONCE, "blockEntityId", () -> -1)
			.uniform1i(ONCE, "currentRenderedItemId", () -> -1)
			.uniform1f(ONCE, "pi", () -> Math.PI)
			.uniform1f(PER_TICK, "playerMood", MINECRAFT_SHIM::getPlayerMood)
			.uniform2i(PER_FRAME, "eyeBrightness", MINECRAFT_SHIM::getEyeBrightness)
			.uniform2i(PER_FRAME, "eyeBrightnessSmooth", () -> {
				Vector2f smoothed = eyeBrightnessSmooth.get();
				return new Vector2i((int) smoothed.x(), (int) smoothed.y());
			})
			.uniform1f(PER_TICK, "rainStrength", MINECRAFT_SHIM::getRainStrength)
			.uniform1f(PER_TICK, "wetness", new SmoothedFloat(directives.getWetnessHalfLife(), directives.getDrynessHalfLife(), MINECRAFT_SHIM::getRainStrength, updateNotifier))
			.uniform3d(PER_FRAME, "skyColor", MINECRAFT_SHIM::getSkyColor)
			.uniform1f(PER_FRAME, "dhFarPlane", DHCompat::getFarPlane)
			.uniform1f(PER_FRAME, "dhNearPlane", DHCompat::getNearPlane)
			.uniform1i(PER_FRAME, "dhRenderDistance", DHCompat::getRenderDistance);
	}

}
