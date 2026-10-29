package net.irisshaders.iris.compat.flywheel.shaderpack;

import net.irisshaders.iris.compat.flywheel.engine.ClrwlOitAccumulateOverride;

import java.util.List;

public record ClrwlOitConfig(boolean enabled, int[] coeffRanks, List<ClrwlOitAccumulateOverride> accumulateOverrides)
{
}
