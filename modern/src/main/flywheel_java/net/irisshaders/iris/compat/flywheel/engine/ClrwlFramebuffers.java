package net.irisshaders.iris.compat.flywheel.engine;

import com.google.common.collect.ImmutableList;
import net.irisshaders.iris.compat.flywheel.compile.ClrwlOitPrograms;
import net.irisshaders.iris.compat.flywheel.shaderpack.ClrwlProgramGroup;
import net.irisshaders.iris.compat.flywheel.shaderpack.ClrwlProgramId;
import net.irisshaders.iris.gl.blending.BufferBlendInformation;
import net.irisshaders.iris.gl.framebuffer.GlFramebuffer;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shaderpack.properties.ProgramDirectives;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ClrwlFramebuffers
{
    private final IrisRenderingPipeline irisPipeline;
    private final ProgramSet programSet;

    private final Map<ClrwlProgramId, GlFramebuffer> framebuffers = new HashMap<>();

    @Nullable
    private ClrwlOitFramebuffers gbuffersOitFramebuffer;
    @Nullable
    private ClrwlOitFramebuffers shadowOitFramebuffer;

    private final Map<ClrwlProgramId, List<BufferBlendInformation>> bufferBlendOverrides = new HashMap<>();

    public ClrwlFramebuffers(IrisRenderingPipeline irisPipeline, ProgramSet programSet)
    {
        this.irisPipeline = irisPipeline;
        this.programSet = programSet;
    }

    @Nullable
    public GlFramebuffer getFramebuffer(ClrwlProgramId program)
    {
        var programSetAccessor = programSet.getClrwlPrograms();

        return framebuffers.computeIfAbsent(program, (key) ->
        {
            var maybeSrc = programSetAccessor.getClrwlProgramSource(key);

            if (maybeSrc.isEmpty())
            {
                return null;
            }

            var src = maybeSrc.get();

            switch (key.group())
            {
                case GBUFFERS ->
                {
                    return irisPipeline.getFlywheelGbuffersFramebuffer(src, program.afterTranslucent());
                }

                case SHADOW ->
                {
                    return irisPipeline.getFlywheelShadowFramebuffer(src);
                }
            }

            throw new RuntimeException("Unknown program group: " + key.group().groupName());
        });
    }

    @Nullable
    public ClrwlOitFramebuffers getOitFramebuffers(ClrwlProgramGroup programGroup, ClrwlOitPrograms oitPrograms, ProgramSet programSet, ProgramDirectives directives)
    {
        switch (programGroup)
        {
            case GBUFFERS ->
            {
                if (gbuffersOitFramebuffer == null)
                {
                    gbuffersOitFramebuffer = new ClrwlOitFramebuffers(programGroup, oitPrograms, irisPipeline, programSet, directives);
                }

                return gbuffersOitFramebuffer;
            }
            case SHADOW ->
            {
                if (shadowOitFramebuffer == null)
                {
                    shadowOitFramebuffer = new ClrwlOitFramebuffers(programGroup, oitPrograms, irisPipeline, programSet, directives);
                }

                return shadowOitFramebuffer;
            }
        }

        throw new RuntimeException("Unknown program group: " + programGroup);
    }

    public Optional<ClrwlBlendModeOverride> getBlendModeOverride(ClrwlProgramId programId)
    {
        var programSetAccessor = programSet.getClrwlPrograms();

        if (!programSetAccessor.isFallbackMode())
        {
            var directives = programSetAccessor.getClrwlDirectives();
            var realProgramId = programSetAccessor.getRealClrwlProgram(programId);

            return realProgramId.flatMap(directives::getBlendModeOverride).or(programId::defaultBlendOverride);
        }
        else
        {
            var maybeSrc = programSetAccessor.getClrwlProgramSource(programId);

            return maybeSrc.flatMap(src -> src.getDirectives().getBlendModeOverride()
                                    .map(bm -> new ClrwlBlendModeOverride(bm.getBlendMode())))
                            .or(programId::defaultBlendOverride);
        }
    }

    public List<BufferBlendInformation> getBufferBlendModeOverrides(ClrwlProgramId programId)
    {
        var programSetAccessor = programSet.getClrwlPrograms();

        if (!programSetAccessor.isFallbackMode())
        {
            var realProgram = programSetAccessor.getRealClrwlProgram(programId);

            if (realProgram.isEmpty())
            {
                return Collections.emptyList();
            }

            return bufferBlendOverrides.computeIfAbsent(realProgram.get(), (key) ->
            {
                var directives = programSetAccessor.getClrwlDirectives();
                var maybeSrc = programSetAccessor.getClrwlProgramSource(key);

                return maybeSrc
                        .map(src -> computeBufferBlendOff(src, directives.getBufferBlendModeOverrides(realProgram.get())))
                        .orElse(Collections.emptyList());
            });
        }
        else
        {
            return bufferBlendOverrides.computeIfAbsent(programId, (key) ->
            {
                var maybeSrc = programSetAccessor.getClrwlProgramSource(programId);

                return maybeSrc
                        .map(src -> src.getDirectives().getBufferBlendOverrides())
                        .orElse(Collections.emptyList());
            });
        }
    }

    private List<BufferBlendInformation> computeBufferBlendOff(ProgramSource source, List<BufferBlendInformation> blendOverrides)
    {
        if (blendOverrides.isEmpty())
        {
            return Collections.emptyList();
        }

        var drawBuffers = source.getDirectives().getDrawBuffers();
        var list = new ArrayList<BufferBlendInformation>();

        for (int i = 0; i < drawBuffers.length; i++)
        {
            int buf = drawBuffers[i];

            for (var entry : blendOverrides)
            {
                if (entry.index() == buf)
                {
                    list.add(new BufferBlendInformation(i, entry.blendMode()));
                }
            }
        }

        return ImmutableList.copyOf(list);
    }

    public void delete()
    {

        if (gbuffersOitFramebuffer != null)
        {
            gbuffersOitFramebuffer.delete();
            gbuffersOitFramebuffer = null;
        }

        if (shadowOitFramebuffer != null)
        {
            shadowOitFramebuffer.delete();
            shadowOitFramebuffer = null;
        }

        // The framebuffers belong to the Iris render targets, which destroy them with the pipeline.
        framebuffers.clear();
        bufferBlendOverrides.clear();
    }
}
