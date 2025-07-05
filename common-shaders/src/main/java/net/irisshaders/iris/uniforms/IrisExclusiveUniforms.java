package net.irisshaders.iris.uniforms;

import net.irisshaders.iris.gl.uniform.UniformHolder;
import net.irisshaders.iris.gl.uniform.UniformUpdateFrequency;
import net.irisshaders.iris.gui.option.IrisVideoSettings;

import static org.embeddedt.embeddium.compat.mc.MinecraftVersionShimService.MINECRAFT_SHIM;

public class IrisExclusiveUniforms {
	public static void addIrisExclusiveUniforms(UniformHolder uniforms) {
		WorldInfoUniforms.addWorldInfoUniforms(uniforms);

		uniforms.uniform1i(UniformUpdateFrequency.PER_TICK, "currentColorSpace", () -> IrisVideoSettings.colorSpace.ordinal());

		// All Iris-exclusive uniforms (uniforms which do not exist in either OptiFine or ShadersMod) should be registered here.
		uniforms.uniform1f(UniformUpdateFrequency.PER_FRAME, "thunderStrength", MINECRAFT_SHIM::getThunderStrength);
		uniforms.uniform1f(UniformUpdateFrequency.PER_TICK, "currentPlayerHealth", MINECRAFT_SHIM::getCurrentHealth);
		uniforms.uniform1f(UniformUpdateFrequency.PER_TICK, "maxPlayerHealth", MINECRAFT_SHIM::getMaxHealth);
		uniforms.uniform1f(UniformUpdateFrequency.PER_TICK, "currentPlayerHunger", MINECRAFT_SHIM::getCurrentHunger);
		uniforms.uniform1f(UniformUpdateFrequency.PER_TICK, "maxPlayerHunger", () -> 20);
		uniforms.uniform1f(UniformUpdateFrequency.PER_TICK, "currentPlayerArmor", MINECRAFT_SHIM::getCurrentArmor);
		uniforms.uniform1f(UniformUpdateFrequency.PER_TICK, "maxPlayerArmor", () -> 50);
		uniforms.uniform1f(UniformUpdateFrequency.PER_TICK, "currentPlayerAir", MINECRAFT_SHIM::getCurrentAir);
		uniforms.uniform1f(UniformUpdateFrequency.PER_TICK, "maxPlayerAir", MINECRAFT_SHIM::getMaxAir);
		uniforms.uniform1b(UniformUpdateFrequency.PER_FRAME, "firstPersonCamera", MINECRAFT_SHIM::isFirstPersonCamera);
		uniforms.uniform1b(UniformUpdateFrequency.PER_TICK, "isSpectator", MINECRAFT_SHIM::isSpectator);
		uniforms.uniform3d(UniformUpdateFrequency.PER_FRAME, "eyePosition", MINECRAFT_SHIM::getEyePosition);
		uniforms.uniform1f(UniformUpdateFrequency.PER_TICK, "cloudTime", CapturedRenderingState.INSTANCE::getCloudTime);
		uniforms.uniform3d(UniformUpdateFrequency.PER_FRAME, "relativeEyePosition", () -> CameraUniforms.getUnshiftedCameraPosition().sub(MINECRAFT_SHIM.getEyePosition()));
		uniforms.uniform3d(UniformUpdateFrequency.PER_FRAME, "playerLookVector", MINECRAFT_SHIM::getPlayerLookVector);
		uniforms.uniform3d(UniformUpdateFrequency.PER_FRAME, "playerBodyVector", MINECRAFT_SHIM::getPlayerBodyVector);
		uniforms.uniform4f(UniformUpdateFrequency.PER_TICK, "lightningBoltPosition", MINECRAFT_SHIM::getLightningBoltPosition);
	}


    public static class WorldInfoUniforms {
		public static void addWorldInfoUniforms(UniformHolder uniforms) {
			uniforms.uniform1i(UniformUpdateFrequency.PER_FRAME, "bedrockLevel", MINECRAFT_SHIM::getBedrockLevel);
			uniforms.uniform1f(UniformUpdateFrequency.PER_FRAME, "cloudHeight", MINECRAFT_SHIM::getCloudHeight);
			uniforms.uniform1i(UniformUpdateFrequency.PER_FRAME, "heightLimit", MINECRAFT_SHIM::getHeightLimit);
			uniforms.uniform1i(UniformUpdateFrequency.PER_FRAME, "logicalHeightLimit", MINECRAFT_SHIM::getLogicalHeightLimit);
			uniforms.uniform1b(UniformUpdateFrequency.PER_FRAME, "hasCeiling", MINECRAFT_SHIM::hasCeiling);
			uniforms.uniform1b(UniformUpdateFrequency.PER_FRAME, "hasSkylight", MINECRAFT_SHIM::hasSkyLight);
			uniforms.uniform1f(UniformUpdateFrequency.PER_FRAME, "ambientLight", MINECRAFT_SHIM::getAmbientLight);

		}
	}
}
