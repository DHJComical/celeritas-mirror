package net.irisshaders.iris.compat.flywheel.shaderpack;

import net.irisshaders.iris.compat.flywheel.engine.ClrwlBlendModeOverride;
import dev.engine_room.flywheel.api.material.Transparency;
import net.irisshaders.iris.shaderpack.loading.ProgramId;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public enum ClrwlProgramId
{
    GBUFFERS(ClrwlProgramGroup.GBUFFERS, "clrwl_gbuffers", ProgramId.Terrain, null, false, null),
    GBUFFERS_ADDITIVE(ClrwlProgramGroup.GBUFFERS,"clrwl_gbuffers_additive", null, GBUFFERS, false, null),
    GBUFFERS_GLINT(ClrwlProgramGroup.GBUFFERS,"clrwl_gbuffers_glint", ProgramId.ArmorGlint, GBUFFERS, false, null),
    GBUFFERS_LIGHTNING(ClrwlProgramGroup.GBUFFERS,"clrwl_gbuffers_lightning", null, GBUFFERS, false, null),
    GBUFFERS_TRANSLUCENT(ClrwlProgramGroup.GBUFFERS,"clrwl_gbuffers_translucent", ProgramId.Water, GBUFFERS, true, null),
    GBUFFERS_DAMAGEDBLOCK(ClrwlProgramGroup.GBUFFERS,"clrwl_gbuffers_damagedblock", ProgramId.DamagedBlock, GBUFFERS, false, null),

    SHADOW(ClrwlProgramGroup.SHADOW, "clrwl_shadow", ProgramId.Shadow, null, false, ClrwlBlendModeOverride.OFF),
    SHADOW_ADDITIVE(ClrwlProgramGroup.SHADOW,"clrwl_shadow_additive", null, SHADOW, false, ClrwlBlendModeOverride.OFF),
    SHADOW_GLINT(ClrwlProgramGroup.SHADOW,"clrwl_shadow_glint", null, SHADOW, false, ClrwlBlendModeOverride.OFF),
    SHADOW_LIGHTNING(ClrwlProgramGroup.SHADOW,"clrwl_shadow_lightning", null, null, false, ClrwlBlendModeOverride.OFF),
    SHADOW_TRANSLUCENT(ClrwlProgramGroup.SHADOW,"clrwl_shadow_translucent", null, SHADOW, true, ClrwlBlendModeOverride.OFF);

    private final ClrwlProgramGroup group;
    private final String name;
    @Nullable
    private final ProgramId fallbackProgram;
    @Nullable
    private final ClrwlProgramId base;
    private final boolean afterTranslucent;
    @Nullable
    private final ClrwlBlendModeOverride defaultBlendOverride;

    ClrwlProgramId(ClrwlProgramGroup group, String name, @Nullable ProgramId fallbackProgram, @Nullable ClrwlProgramId base, boolean afterTranslucent, @Nullable ClrwlBlendModeOverride defaultBlendOverride)
    {
        this.group = group;
        this.name = name;
        this.fallbackProgram = fallbackProgram;
        this.base = base;
        this.afterTranslucent = afterTranslucent;
        this.defaultBlendOverride = defaultBlendOverride;
    }

    public ClrwlProgramGroup group()
    {
        return group;
    }

    public String programName()
    {
        return name;
    }

    @Nullable
    public ProgramId fallbackProgram()
    {
        return fallbackProgram;
    }

    @Nullable
    public ClrwlProgramId base()
    {
        return base;
    }

    public boolean afterTranslucent()
    {
        return afterTranslucent;
    }

    public Optional<ClrwlBlendModeOverride> defaultBlendOverride()
    {
        return Optional.ofNullable(defaultBlendOverride);
    }

    public static ClrwlProgramId[] gbuffers()
    {
        return new ClrwlProgramId[] { GBUFFERS, GBUFFERS_ADDITIVE, GBUFFERS_GLINT, GBUFFERS_LIGHTNING, GBUFFERS_TRANSLUCENT, GBUFFERS_DAMAGEDBLOCK };
    }

    public static ClrwlProgramId[] shadow()
    {
        return new ClrwlProgramId[] { SHADOW, SHADOW_ADDITIVE, SHADOW_GLINT, SHADOW_LIGHTNING, SHADOW_TRANSLUCENT };
    }

    public static Optional<ClrwlProgramId> fromName(String name)
    {
        for (var program : values())
        {
            if (program.name.equals(name))
            {
                return Optional.of(program);
            }
        }

        return Optional.empty();
    }

    public static ClrwlProgramId fromTransparency(Transparency transparency, boolean isShadow)
    {
        if (!isShadow)
        {
            switch (transparency)
            {
                case OPAQUE ->
                {
                    return ClrwlProgramId.GBUFFERS;
                }
                case ADDITIVE ->
                {
                    return ClrwlProgramId.GBUFFERS_ADDITIVE;
                }
                case LIGHTNING ->
                {
                    return ClrwlProgramId.GBUFFERS_LIGHTNING;
                }
                case GLINT ->
                {
                    return ClrwlProgramId.GBUFFERS_GLINT;
                }
                case CRUMBLING ->
                {
                    return ClrwlProgramId.GBUFFERS_DAMAGEDBLOCK;
                }
                case TRANSLUCENT, ORDER_INDEPENDENT ->
                {
                    return ClrwlProgramId.GBUFFERS_TRANSLUCENT;
                }
            }
        }
        else
        {
            switch (transparency)
            {
                case OPAQUE ->
                {
                    return ClrwlProgramId.SHADOW;
                }
                case ADDITIVE ->
                {
                    return ClrwlProgramId.SHADOW_ADDITIVE;
                }
                case LIGHTNING ->
                {
                    return ClrwlProgramId.SHADOW_LIGHTNING;
                }
                case GLINT ->
                {
                    return ClrwlProgramId.SHADOW_GLINT;
                }
                case TRANSLUCENT, ORDER_INDEPENDENT ->
                {
                    return ClrwlProgramId.SHADOW_TRANSLUCENT;
                }
                case CRUMBLING ->
                {
                    // Should never happen
                    return ClrwlProgramId.SHADOW;
                }
            }
        }

        throw new RuntimeException("Unknown transparency: " + transparency);
    }
}
