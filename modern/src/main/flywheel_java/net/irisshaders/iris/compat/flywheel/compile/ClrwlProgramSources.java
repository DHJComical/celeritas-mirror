package net.irisshaders.iris.compat.flywheel.compile;

import net.irisshaders.iris.compat.flywheel.compile.transform.ClrwlComputeTransformPatcher;
import net.irisshaders.iris.compat.flywheel.compile.transform.ClrwlTransformOutput;
import net.irisshaders.iris.compat.flywheel.compile.transform.ClrwlTransformPatcher;
import net.irisshaders.iris.compat.flywheel.gl.ClrwlShaderType;
import net.irisshaders.iris.compat.flywheel.shaderpack.ClrwlPackPrograms;
import net.irisshaders.iris.compat.flywheel.shaderpack.ClrwlProgramId;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.shaderpack.programs.ComputeSource;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import org.embeddedt.embeddium.impl.gl.shader.ShaderType;

import java.util.*;

public class ClrwlProgramSources
{
    private final IrisRenderingPipeline irisPipeline;
    private final ProgramSet programSet;
    private final Map<GbuffersKey, PatchedGbuffersSources> gbuffersSources;
    private final Map<ComputeKey, PatchedComputeSource> computeSources;

    public ClrwlProgramSources(IrisRenderingPipeline irisPipeline, ProgramSet programSet)
    {
        this.irisPipeline = irisPipeline;
        this.programSet = programSet;
        this.gbuffersSources = new HashMap<>();
        this.computeSources = new HashMap<>();
    }

    public PatchedGbuffersSources getGbuffersSources(ClrwlProgramId programId, ClrwlPrograms.OitMode oit)
    {
        ClrwlPackPrograms programAccessor = programSet.getClrwlPrograms();
        ClrwlProgramId realProgramId = programAccessor.getRealClrwlProgram(programId).orElseThrow();

        var isCrumbling = programId == ClrwlProgramId.GBUFFERS_DAMAGEDBLOCK;
        var customOutputs = oit == ClrwlPrograms.OitMode.DEPTH_RANGE || oit == ClrwlPrograms.OitMode.GENERATE_COEFFICIENTS;

        var key = new GbuffersKey(realProgramId, isCrumbling, customOutputs);

        return gbuffersSources.computeIfAbsent(key, this::patchGbuffersSources);
    }

    public PatchedComputeSource getComputeSource(ComputeSource source)
    {
        var key = new ComputeKey(source.getSource().orElseThrow());
        return computeSources.computeIfAbsent(key, this::patchComputeSource);
    }

    private PatchedGbuffersSources patchGbuffersSources(GbuffersKey k)
    {
        ClrwlPackPrograms programAccessor = programSet.getClrwlPrograms();
        ProgramSource sources = programAccessor.getClrwlProgramSource(k.realProgramId()).orElseThrow();
        EnumMap<ClrwlShaderType, List<String>> extensions = new EnumMap<>(ClrwlShaderType.class);
        extensions.put(ClrwlShaderType.VERTEX, parseShaderExtensions(sources.getSourceNullable(ShaderType.VERTEX)));
        extensions.put(ClrwlShaderType.GEOMETRY, parseShaderExtensions(sources.getSourceNullable(ShaderType.GEOM)));
        extensions.put(ClrwlShaderType.FRAGMENT, parseShaderExtensions(sources.getSourceNullable(ShaderType.FRAGMENT)));
        int[] drawBuffers = sources.getDirectives().getDrawBuffers();

        String vertexSource = ClrwlTransformPatcher.patchVertex(sources.getSource(ShaderType.VERTEX).orElseThrow(), k.isCrumbling(), irisPipeline.getTextureMap());
        Optional<String> geometrySource = sources.getSource(ShaderType.GEOM).map(s -> ClrwlTransformPatcher.patchGeometry(s, k.isCrumbling(), irisPipeline.getTextureMap()));
        ClrwlTransformOutput fragmentSource = ClrwlTransformPatcher.patchFragment(sources.getSource(ShaderType.FRAGMENT).orElseThrow(), k.isCrumbling(), k.customOutputs(), irisPipeline.getTextureMap());

        return new PatchedGbuffersSources(vertexSource, geometrySource, fragmentSource, extensions, drawBuffers);
    }

    private PatchedComputeSource patchComputeSource(ComputeKey k)
    {
        var patchedSrc = ClrwlComputeTransformPatcher.patchCompute(k.shader(), irisPipeline.getTextureMap());
        return new PatchedComputeSource(patchedSrc);
    }

    private static List<String> parseShaderExtensions(String source)
    {
        List<String> res = new ArrayList<>();

        if (source == null)
        {
            return res;
        }

        source.lines().forEach(line ->
        {
            line = line.trim();

            if (line.startsWith("#extension ") && line.contains(":"))
            {
                res.add(line.substring(11).split(":")[0].trim());
            }
        });

        return res;
    }

    private record GbuffersKey(ClrwlProgramId realProgramId, boolean isCrumbling, boolean customOutputs)
    {
    }

    public record PatchedGbuffersSources(String vertex, Optional<String> geometry, ClrwlTransformOutput fragment,
                                         EnumMap<ClrwlShaderType, List<String>> extensions, int[] drawBuffers)
    {
    }

    public record ComputeKey(String shader)
    {
    }

    public record PatchedComputeSource(String shader)
    {
    }
}
