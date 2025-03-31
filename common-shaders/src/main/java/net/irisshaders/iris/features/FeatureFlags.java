package net.irisshaders.iris.features;

import net.irisshaders.iris.gl.IrisRenderSystem;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;

import static org.embeddedt.embeddium.compat.mc.MinecraftVersionShimService.MINECRAFT_SHIM;

public enum FeatureFlags {
	SEPARATE_HARDWARE_SAMPLERS(() -> true, () -> true),
	HIGHER_SHADOWCOLOR(() -> true, () -> true),
	CUSTOM_IMAGES(() -> true, IrisRenderSystem::supportsImageLoadStore),
	PER_BUFFER_BLENDING(() -> true, IrisRenderSystem::supportsBufferBlending),
	COMPUTE_SHADERS(() -> true, IrisRenderSystem::supportsCompute),
	TESSELLATION_SHADERS(() -> true, IrisRenderSystem::supportsTesselation),
	ENTITY_TRANSLUCENT(() -> true, () -> true),
	REVERSED_CULLING(() -> true, () -> true),
	BLOCK_EMISSION_ATTRIBUTE(() -> true, () -> true),
	SSBO(() -> true, IrisRenderSystem::supportsSSBO),
	UNKNOWN(() -> false, () -> false);

	private final BooleanSupplier irisRequirement;
	private final BooleanSupplier hardwareRequirement;

	FeatureFlags(BooleanSupplier irisRequirement, BooleanSupplier hardwareRequirement) {
		this.irisRequirement = irisRequirement;
		this.hardwareRequirement = hardwareRequirement;
	}

	public static String getInvalidStatus(List<FeatureFlags> invalidFeatureFlags) {
		boolean unsupportedHardware = false, unsupportedIris = false;
		FeatureFlags[] flags = invalidFeatureFlags.toArray(new FeatureFlags[0]);
		for (FeatureFlags flag : flags) {
			unsupportedIris |= !flag.irisRequirement.getAsBoolean();
			unsupportedHardware |= !flag.hardwareRequirement.getAsBoolean();
		}

		if (unsupportedIris) {
			if (unsupportedHardware) {
				return MINECRAFT_SHIM.translate("iris.unsupported.irisorpc");
			}

			return MINECRAFT_SHIM.translate("iris.unsupported.iris");
		} else if (unsupportedHardware) {
			return MINECRAFT_SHIM.translate("iris.unsupported.pc");
		} else {
			return null;
		}
	}

	public static boolean isInvalid(String name) {
		try {
			return !FeatureFlags.valueOf(name.toUpperCase(Locale.US)).isUsable();
		} catch (IllegalArgumentException e) {
			return true;
		}
	}

	public static FeatureFlags getValue(String value) {
        if(value.equalsIgnoreCase("TESSELATION_SHADERS")) {
            value = "TESSELLATION_SHADERS";
        }

		try {
			return FeatureFlags.valueOf(value.toUpperCase(Locale.US));
		} catch (IllegalArgumentException e) {
			return FeatureFlags.UNKNOWN;
		}
	}

	public String getHumanReadableName() {
		return StringUtils.capitalize(name().replace("_", " ").toLowerCase());
	}

	public boolean isUsable() {
		return irisRequirement.getAsBoolean() && hardwareRequirement.getAsBoolean();
	}
}
