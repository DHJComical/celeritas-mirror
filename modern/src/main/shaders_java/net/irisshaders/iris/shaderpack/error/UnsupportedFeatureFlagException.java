package net.irisshaders.iris.shaderpack.error;

import lombok.Getter;
import net.irisshaders.iris.features.FeatureFlags;

import java.util.List;
import java.util.stream.Collectors;

public class UnsupportedFeatureFlagException extends Exception {
    @Getter
    private final List<FeatureFlags> unsupportedFeatureFlags;

    public UnsupportedFeatureFlagException(List<FeatureFlags> unsupportedFeatureFlags) {
        super("The shader pack being loaded requires unsupported features: " + unsupportedFeatureFlags.stream().map(FeatureFlags::name).collect(Collectors.joining(", ")));
        this.unsupportedFeatureFlags = List.copyOf(unsupportedFeatureFlags);
    }
}
