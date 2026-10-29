package net.irisshaders.iris.compat.flywheel.compile;

import org.embeddedt.embeddium.impl.gl.shader.ShaderType;
import net.irisshaders.iris.compat.flywheel.Colorwheel;
import net.irisshaders.iris.compat.flywheel.compile.component.*;
import net.irisshaders.iris.compat.flywheel.compile.core.ClrwlCompilation;
import net.irisshaders.iris.compat.flywheel.compile.core.ClrwlCompilationHarness;
import net.irisshaders.iris.compat.flywheel.compile.core.ClrwlCompile;
import net.irisshaders.iris.compat.flywheel.compile.core.ClrwlShaderSources;
import net.irisshaders.iris.compat.flywheel.engine.ClrwlOitAccumulateOverride;
import net.irisshaders.iris.compat.flywheel.engine.ClrwlVertex;
import net.irisshaders.iris.compat.flywheel.gl.ClrwlShaderType;
import net.irisshaders.iris.compat.flywheel.shaderpack.ClrwlPackDirectives;
import net.irisshaders.iris.compat.flywheel.shaderpack.ClrwlProgramGroup;
import net.irisshaders.iris.compat.flywheel.shaderpack.ClrwlProgramId;
import net.irisshaders.iris.compat.flywheel.util.Utils;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.backend.BackendConfig;
import dev.engine_room.flywheel.backend.compile.component.InstanceStructComponent;
import dev.engine_room.flywheel.backend.gl.GlCompat;
import dev.engine_room.flywheel.backend.glsl.GlslVersion;
import dev.engine_room.flywheel.backend.glsl.SourceComponent;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.util.ResourceUtil;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.irisshaders.iris.shaderpack.properties.PackDirectives;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ClrwlPrograms
{
    public enum OitMode
    {
        OFF("", ""),
        DEPTH_RANGE("CLRWL_DEPTH_RANGE", "_depth_range"),
        GENERATE_COEFFICIENTS("CLRWL_COLLECT_COEFFS", "_generate_coefficients"),
        EVALUATE("CLRWL_EVALUATE", "_resolve"),
        ;

        public final String define;
        public final String name;

        OitMode(String define, String name)
        {
            this.define = define;
            this.name = name;
        }
    }

    public record Pipeline(String id, boolean fallback,
                           Collection<String> extensions, InstanceAssembler assembler,
                           ResourceLocation vertex, ResourceLocation geometry, ResourceLocation fragment)
    {
        public interface InstanceAssembler
        {
            SourceComponent assemble(InstanceType<?> type);
        }
    }

    public static final ResourceLocation API_IMPL_VERT = Colorwheel.rl("internal/api_impl.vert");
    public static final ResourceLocation API_IMPL_GEOM = Colorwheel.rl("internal/api_impl_geom.glsl");
    public static final ResourceLocation API_IMPL_FRAG = Colorwheel.rl("internal/api_impl.frag");

    public static final ResourceLocation IRIS_COMPAT_VERT = Colorwheel.rl("internal/iris_compat.vert");
    public static final ResourceLocation IRIS_COMPAT_GEOM = Colorwheel.rl("internal/iris_compat_geom.glsl");
    public static final ResourceLocation IRIS_COMPAT_FRAG = Colorwheel.rl("internal/iris_compat.frag");

    public static final ResourceLocation OIT_DEPTH_RANGE_FRAG = Colorwheel.rl("internal/oit/depth_range.frag");

    public static final String CLRWL_POST_FRAGMENT_FCT = "_clrwl_post_shader";

    private static final ClrwlCompile<ClrwlShaderKey, ClrwlProgram> PIPELINE = new ClrwlCompile<>();

    private final ClrwlCompilationHarness<ClrwlShaderKey, ClrwlProgram> pipelineHarness;
    private final ProgramSet programSet;

    public ClrwlPrograms(ClrwlShaderSources sources, Pipeline pipeline)
    {
        this.pipelineHarness = createPipeline(sources, pipeline);
        this.programSet = sources.programSet();
    }

    private static ClrwlCompilationHarness<ClrwlShaderKey, ClrwlProgram> createPipeline(ClrwlShaderSources sources, Pipeline pipeline)
    {
        ClrwlPackDirectives directives = sources.programSet().getClrwlPrograms().getClrwlDirectives();

        var vert = PIPELINE.shader(GlCompat.MAX_GLSL_VERSION, ClrwlShaderType.VERTEX)
                .nameMapper(k ->
                {
                    var program = getProgram(k);
                    var instance = ResourceUtil.toDebugFileNameNoExtension(k.instanceType().vertexShader());
                    var material = ResourceUtil.toDebugFileNameNoExtension(k.material().vertexSource());
                    var context = k.context().nameLowerCase();
                    var debug = k.isDebugEnabled() ? "_debug" : "";
                    return "pipeline/" + pipeline.id() + "/" + program.programName() + "/vert/" + instance + "/" + material + "_" + context + debug;
                })
                .onCompile((k, c) -> setDefines(k, c, sources, pipeline, ClrwlShaderType.VERTEX))
                .requireExtensions(pipeline.extensions())
                .withResource(API_IMPL_VERT)
                .withComponent(k -> new InstanceStructComponent(k.instanceType()))
                .withResource(k -> k.instanceType().vertexShader())
                .withResource(k -> k.material().vertexSource())
                .withResource(ClrwlVertex.LAYOUT_SHADER)
                .withResource(IRIS_COMPAT_VERT)
                .withComponent(k -> pipeline.assembler().assemble(k.instanceType()))
                .withComponent(k -> getIrisShaderSource(k, sources, pipeline, ClrwlShaderType.VERTEX))
                .withResource(pipeline.vertex());

        var geom = PIPELINE.shader(GlCompat.MAX_GLSL_VERSION, ClrwlShaderType.GEOMETRY)
                .nameMapper(k ->
                {
                    var program = getProgram(k);
                    var instance = ResourceUtil.toDebugFileNameNoExtension(k.instanceType().vertexShader());
                    var material = ResourceUtil.toDebugFileNameNoExtension(k.material().vertexSource());
                    var context = k.context().nameLowerCase();
                    var debug = k.isDebugEnabled() ? "_debug" : "";
                    return "pipeline/" + pipeline.id() + "/" + program.programName() + "/geom/" + instance + "/" + material + "_" + context + debug;
                })
                .condition(ClrwlPrograms::hasGeometryShader)
                .onCompile((k, c) -> setDefines(k, c, sources, pipeline, ClrwlShaderType.GEOMETRY))
                .requireExtensions(pipeline.extensions())
                .withResource(API_IMPL_GEOM)
                .withResource(IRIS_COMPAT_GEOM)
                .withComponent(k -> getIrisShaderSource(k, sources, pipeline, ClrwlShaderType.GEOMETRY))
                .withResource(pipeline.geometry());

        var frag = PIPELINE.shader(GlCompat.MAX_GLSL_VERSION, ClrwlShaderType.FRAGMENT)
                .nameMapper(k ->
                {
                    var program = getProgram(k);
                    var context = k.context().nameLowerCase();
                    var material = ResourceUtil.toDebugFileNameNoExtension(k.material().fragmentSource());
                    var light = ResourceUtil.toDebugFileNameNoExtension(k.light().source());
                    var debug = k.isDebugEnabled() ? "_debug" : "";
                    var cutout = k.cutout() != CutoutShaders.OFF ? "_cutout" : "";
                    var oit = k.oit().name;
                    return "pipeline/" + pipeline.id() + "/" + program.programName() + "/frag/" + material + "/" + light + "_" + context + cutout + debug + oit;
                })
                .onCompile((k, c) -> setDefines(k, c, sources, pipeline, ClrwlShaderType.FRAGMENT))
                .requireExtensions(pipeline.extensions())
                .enableExtension("GL_ARB_conservative_depth")
                .withResource(API_IMPL_FRAG)
                .withResource(k -> k.cutout().source());

        if (!pipeline.fallback())
        {
            frag.withResource(k -> k.material().fragmentSource())
                .withResource(k -> k.fog().source())
                .withResource(k -> k.light().source())
                .withResource(IRIS_COMPAT_FRAG)
                .withComponent(k -> getOitInouts(k, directives))
                .with((k, src) -> getIrisShaderSource(k, src, pipeline, ClrwlShaderType.FRAGMENT))
                .withResource(pipeline.fragment())
                .with((k, src) -> getPostShaderFragmentSource(k, src, pipeline, directives));
        }
        else
        {
            frag.withResource(IRIS_COMPAT_FRAG)
                .withComponent(k -> getIrisShaderSource(k, sources, pipeline, ClrwlShaderType.FRAGMENT))
                .withResource(pipeline.fragment())
                .with((k, src) -> getFallbackCutoutFragmentSource(k, src, pipeline));
        }

        return PIPELINE.program()
                .link(vert).link(geom).link(frag)
                .preLink(($, p) -> p.preLink())
                .postLink(($, p) -> p.postLink(sources.irisPipeline(), directives))
                .harness(pipeline.id(), sources, (k, h) ->
                {
                    var instanceName = ResourceUtil.toDebugFileNameNoExtension(k.instanceType().vertexShader());
                    var materialName = ResourceUtil.toDebugFileNameNoExtension(k.material().vertexSource());
                    var contextName = k.context().nameLowerCase();
                    var oitName = k.oit().name;
                    var program = getProgram(k);

                    String name = String.format("%s/%s/%s_%s%s", program.programName(), instanceName, materialName, contextName, oitName);

                    return new ClrwlProgram(name, program, h);
                });
    }

    private static void setDefines(ClrwlShaderKey k, ClrwlCompilation c, ClrwlShaderSources sources, Pipeline pipeline, ClrwlShaderType type)
    {
        c.define("CLRWL_IS_" + pipeline.id().toUpperCase(Locale.ROOT));

        defineClrwlPass(k.isShadow(), c);
        defineFmaFallback(c, pipeline.extensions());

        if (pipeline.fallback())
        {
            c.define("_CLRWL_IS_FALLBACK");
        }
        else if (k.oit() != OitMode.OFF)
        {
            c.define("CLRWL_OIT");
            c.define(k.oit().define);
        }

        if (k.isDebugEnabled())
        {
            c.define("_FLW_DEBUG");
        }

        if (k.cutout() != CutoutShaders.OFF)
        {
            c.define("_FLW_USE_DISCARD");
        }

        switch (k.context())
        {
            case DEFAULT -> {}
            case CRUMBLING -> c.define("_FLW_CRUMBLING");
            case EMBEDDED -> c.define("FLW_EMBEDDED");
        }

        switch (BackendConfig.INSTANCE.lightSmoothness())
        {
            case FLAT ->c.define("_FLW_LIGHT_SMOOTHNESS", "0");
            case TRI_LINEAR -> c.define("_FLW_LIGHT_SMOOTHNESS", "1");
            case SMOOTH -> c.define("_FLW_LIGHT_SMOOTHNESS", "2");
            case SMOOTH_INNER_FACE_CORRECTED ->
            {
                c.define("_FLW_LIGHT_SMOOTHNESS", "2");
                c.define("_FLW_INNER_FACE_CORRECTION");
            }
        }

        PackDirectives packDirectives = sources.programSet().getPackDirectives();

        if (packDirectives.isOldLighting())
        {
            c.define("CLRWL_OLD_LIGHTING");
        }

        if (packDirectives.shouldUseSeparateAo())
        {
            c.define("_CLRWL_SEPARATE_AO");
        }

        var program = getProgram(k);
        var src = sources.clrwlSources().getGbuffersSources(program, k.oit());
        var exts = src.extensions().get(type);

        for (var ext : exts)
        {
            c.enableExtension(ext);
        }
    }

    public static void defineClrwlPass(boolean isShadow, ClrwlCompilation c)
    {
        if (isShadow)
        {
            c.define("_CLRWL_IS_SHADOW_PASS");
        }
        else
        {
            c.define("_CLRWL_IS_GBUFFERS_PASS");
        }
    }

    private static void defineFmaFallback(ClrwlCompilation c, Collection<String> extensions)
    {
        if (GlCompat.MAX_GLSL_VERSION.compareTo(GlslVersion.V400) < 0 && !extensions.contains("GL_ARB_gpu_shader5"))
        {
            c.define("fma(a, b, c)", "((a) * (b) + (c))");
        }
    }

    private static boolean hasGeometryShader(ClrwlShaderKey key, ClrwlShaderSources sources)
    {
        var program = getProgram(key);
        var maybeSrc = sources.programSet().getClrwlPrograms().getClrwlProgramSource(program);
        return maybeSrc.map(src -> src.getSource(ShaderType.GEOM).isPresent()).orElse(false);
    }

    private static ClrwlProgramId getProgram(ClrwlShaderKey k)
    {
        return ClrwlProgramId.fromTransparency(k.transparency(), k.isShadow());
    }

    private static SourceComponent getIrisShaderSource(ClrwlShaderKey k, ClrwlShaderSources sources, Pipeline pipeline, ClrwlShaderType type)
    {
        var program = getProgram(k);
        var src = sources.clrwlSources().getGbuffersSources(program, k.oit());
        var name = program.programName();

        switch (type)
        {
            case VERTEX ->   { return new IrisShaderComponent(name + ".vsh", src.vertex()); }
            case GEOMETRY -> { return new IrisShaderComponent(name + ".gsh", src.geometry().orElseThrow()); }
            case FRAGMENT -> { return new IrisShaderComponent(name + ".fsh", src.fragment().code()); }
        }

        throw new RuntimeException("Got unexpected ShaderType: " + type);
    }

    private static SourceComponent getOitInouts(ClrwlShaderKey k, ClrwlPackDirectives directives)
    {
        var programGroup = ClrwlProgramGroup.fromShadow(k.isShadow());

        if (k.oit() == OitMode.GENERATE_COEFFICIENTS)
        {
            var ranks = directives.getOitConfig(programGroup).coeffRanks();
            return new OitCoefficientsOutputComponent(ranks);
        }
        else if (k.oit() == OitMode.EVALUATE)
        {
            var ranks = directives.getOitConfig(programGroup).coeffRanks();
            return new OitCoefficientsSamplersComponent(ranks.length);
        }
        else
        {
            return new EmptyComponent();
        }
    }

    private static SourceComponent getPostShaderFragmentSource(ClrwlShaderKey k, ClrwlShaderSources sources, Pipeline pipeline, ClrwlPackDirectives directives)
    {
        var programId = getProgram(k);
        var programGroup = programId.group();

        switch (k.oit())
        {
            case DEPTH_RANGE ->
            {
                return sources.flwSources().get(OIT_DEPTH_RANGE_FRAG);
            }

            case GENERATE_COEFFICIENTS ->
            {
                var src = sources.clrwlSources().getGbuffersSources(programId, k.oit());
                var drawBuffers = src.drawBuffers();
                var config = directives.getOitConfig(programGroup);

                Map<Integer, Integer> coeffFrag = new HashMap<>();

                for (int i = 0; i < drawBuffers.length; i++)
                {
                    int buffer = drawBuffers[i];
                    var maybeCoeffId = Utils.findFirst(config.accumulateOverrides(), e -> e.drawBuffer() == buffer)
                            .flatMap(ClrwlOitAccumulateOverride::coefficientId);

                    if (maybeCoeffId.isPresent())
                    {
                        coeffFrag.putIfAbsent(maybeCoeffId.get(), i);
                    }
                }

                return new OitCollectCoeffsComponent(config.coeffRanks(), coeffFrag, src.fragment().outputs());
            }

            case EVALUATE ->
            {
                var src = sources.clrwlSources().getGbuffersSources(programId, k.oit());
                var drawBuffers = src.drawBuffers();
                var config = directives.getOitConfig(programGroup);
                var outputs = src.fragment().outputs();

                return new OitEvaluateComponent(drawBuffers, config.coeffRanks(), config.accumulateOverrides(), outputs);
            }
        }

        return new NoopFunctionComponent(CLRWL_POST_FRAGMENT_FCT);
    }

    private static SourceComponent getFallbackCutoutFragmentSource(ClrwlShaderKey k, ClrwlShaderSources sources, Pipeline pipeline)
    {
        var programId = getProgram(k);
        var src = sources.clrwlSources().getGbuffersSources(programId, k.oit());
        return new FallbackCutoutComponent(src.fragment().outputs());
    }

    @Nullable
    public ClrwlProgram get(ClrwlShaderKey key)
    {
        if (brokenShaders.contains(key))
        {
            return null;
        }

        try
        {
            return pipelineHarness.get(key);
        }
        catch (Exception e)
        {
            handleBrokenShader(key, e);
            return null;
        }
    }

    private final Set<ClrwlShaderKey> brokenShaders = new HashSet<>();

    private void handleBrokenShader(ClrwlShaderKey key, Exception e)
    {
        var program = getProgram(key);

        brokenShaders.add(key);

        ClrwlProgramId realProgramId = programSet.getClrwlPrograms().getRealClrwlProgram(program).orElse(program);
        String shaderPath = realProgramId.programName() + "/" + key.getPath();

        Colorwheel.LOGGER.error("Could not compile shader: " + shaderPath, e);
    }

    public void delete()
    {
        pipelineHarness.delete();
    }
}
