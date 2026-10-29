package net.irisshaders.iris.compat.flywheel.compile.core;

import net.irisshaders.iris.compat.flywheel.compile.ClrwlProgramSources;
import dev.engine_room.flywheel.backend.glsl.ShaderSources;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;

public class ClrwlShaderSources
{
    private final ShaderSources flwSources;
    private final ProgramSet programSet;
    private final ClrwlProgramSources clrwlSources;
    private final IrisRenderingPipeline irisPipeline;

    public ClrwlShaderSources(ShaderSources flwSources, ProgramSet programSet, IrisRenderingPipeline irisPipeline)
    {
        this.flwSources = flwSources;
        this.programSet = programSet;
        this.irisPipeline = irisPipeline;
        this.clrwlSources = new ClrwlProgramSources(irisPipeline, programSet);
    }

    public ShaderSources flwSources()
    {
        return flwSources;
    }

    public ProgramSet programSet()
    {
        return programSet;
    }

    public IrisRenderingPipeline irisPipeline()
    {
        return irisPipeline;
    }

    public ClrwlProgramSources clrwlSources()
    {
        return clrwlSources;
    }
}
