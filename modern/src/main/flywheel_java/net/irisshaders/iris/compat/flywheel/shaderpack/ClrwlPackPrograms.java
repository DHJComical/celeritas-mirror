package net.irisshaders.iris.compat.flywheel.shaderpack;

import net.irisshaders.iris.shaderpack.include.AbsolutePackPath;
import net.irisshaders.iris.shaderpack.loading.ProgramId;
import net.irisshaders.iris.shaderpack.programs.ComputeSource;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shaderpack.properties.ShaderProperties;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * The Flywheel programs of one {@link ProgramSet}. Packs that ship no {@code clrwl_gbuffers} program run in
 * fallback mode, where each Flywheel program borrows the closest vanilla gbuffers program.
 */
public class ClrwlPackPrograms
{
    private final ProgramSet programSet;
    private final Map<ClrwlProgramId, ProgramSource> programSrcs = new EnumMap<>(ClrwlProgramId.class);
    @Nullable
    private final ComputeSource shadowDistortSrc;
    private final boolean isFallbackMode;
    private final ClrwlPackDirectives directives;

    public ClrwlPackPrograms(AbsolutePackPath directory, Function<AbsolutePackPath, String> sourceProvider, ProgramSet programSet,
                             ShaderProperties shaderProperties, ClrwlShaderProperties clrwlProperties)
    {
        this.programSet = programSet;

        var clrwlGbuffers = ProgramSet.readProgramSource(directory, sourceProvider, ClrwlProgramId.GBUFFERS.programName(), programSet, shaderProperties, false)
                .requireValid();
        ComputeSource shadowDistort = null;

        if (clrwlGbuffers.isPresent())
        {
            isFallbackMode = false;
            programSrcs.put(ClrwlProgramId.GBUFFERS, clrwlGbuffers.get());

            for (var program : ClrwlProgramId.values())
            {
                if (program == ClrwlProgramId.GBUFFERS)
                {
                    continue;
                }

                ProgramSet.readProgramSource(directory, sourceProvider, program.programName(), programSet, shaderProperties, false)
                        .requireValid()
                        .ifPresent(programSource -> programSrcs.put(program, programSource));
            }

            var shadowTransform = ProgramSet.readComputeSource(directory, sourceProvider, "clrwl_shadow_distort", programSet, shaderProperties);

            if (shadowTransform != null && shadowTransform.isValid())
            {
                shadowDistort = shadowTransform;
            }
        }
        else
        {
            isFallbackMode = true;

            for (var program : ClrwlProgramId.values())
            {
                getFallbackProgramSrc(program.fallbackProgram())
                        .ifPresent(programSource -> programSrcs.put(program, programSource));
            }
        }

        this.shadowDistortSrc = shadowDistort;
        this.directives = new ClrwlPackDirectives(programSet, this, clrwlProperties);
    }

    private Optional<ProgramSource> getFallbackProgramSrc(@Nullable ProgramId programId)
    {
        while (programId != null)
        {
            var src = programSet.get(programId);

            if (src.isPresent())
            {
                return src;
            }

            programId = programId.getFallback().orElse(null);
        }

        return Optional.empty();
    }

    public ClrwlPackDirectives getClrwlDirectives()
    {
        return directives;
    }

    public Optional<ClrwlProgramId> getRealClrwlProgram(ClrwlProgramId programId)
    {
        ClrwlProgramId cur = programId;

        while (cur != null)
        {
            if (programSrcs.containsKey(cur))
            {
                return Optional.of(cur);
            }

            cur = cur.base();
        }

        return Optional.empty();
    }

    public Optional<ProgramId> getRealFallbackProgram(ClrwlProgramId programId)
    {
        var clrwlCur = programId;

        while (clrwlCur != null && clrwlCur.fallbackProgram() == null)
        {
            clrwlCur = clrwlCur.base();
        }

        if (clrwlCur == null)
        {
            return Optional.empty();
        }

        var cur = Optional.of(clrwlCur.fallbackProgram());

        while (cur.isPresent())
        {
            if (programSet.get(cur.get()).isPresent())
            {
                return cur;
            }

            cur = cur.get().getFallback();
        }

        return Optional.empty();
    }

    public Optional<ProgramSource> getClrwlProgramSource(ClrwlProgramId programId)
    {
        return getRealClrwlProgram(programId).map(programSrcs::get);
    }

    public Optional<ComputeSource> getShadowDistortSource()
    {
        return Optional.ofNullable(shadowDistortSrc);
    }

    public boolean isFallbackMode()
    {
        return isFallbackMode;
    }
}
