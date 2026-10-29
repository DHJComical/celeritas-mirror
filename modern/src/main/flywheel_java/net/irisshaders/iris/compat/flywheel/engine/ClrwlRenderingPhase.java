package net.irisshaders.iris.compat.flywheel.engine;

public enum ClrwlRenderingPhase
{
    SOLID,
    TRANSLUCENT,
    OIT_DEPTH_RANGE,
    OIT_COEFFICIENTS,
    OIT_ACCUMULATE,
    OIT_COMPOSITE,
    CRUMBLING,
    STAGING_BUFFER_FLUSH,
    INDIRECT_DEPTH_PYRAMID,
    INDIRECT_CULL,
    INDIRECT_CULL_APPLY,
    INDIRECT_CULL_RESET,
    INDIRECT_CULL_TRANSFORM;

    public int getValue()
    {
        // ordinal is shifted to prevent collision with Iris or other mod
        return 110800 + ordinal();
    }
}
