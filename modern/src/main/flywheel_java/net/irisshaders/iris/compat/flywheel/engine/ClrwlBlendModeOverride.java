package net.irisshaders.iris.compat.flywheel.engine;

import net.irisshaders.iris.gl.blending.BlendMode;
import org.jetbrains.annotations.Nullable;

public record ClrwlBlendModeOverride(@Nullable BlendMode blendMode)
{
    public static final ClrwlBlendModeOverride OFF = new ClrwlBlendModeOverride(null);
}
