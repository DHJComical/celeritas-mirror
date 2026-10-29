package net.irisshaders.iris.compat.flywheel.compile;

import net.irisshaders.iris.compat.flywheel.ClrwlSamplers;
import net.irisshaders.iris.compat.flywheel.compile.component.OitCompositeComponent;
import net.irisshaders.iris.compat.flywheel.engine.ClrwlOitAccumulateOverride;
import net.irisshaders.iris.compat.flywheel.engine.uniform.ClrwlUniforms;
import dev.engine_room.flywheel.backend.compile.core.Compilation;
import dev.engine_room.flywheel.backend.compile.core.CompilationHarness;
import dev.engine_room.flywheel.backend.compile.core.Compile;
import dev.engine_room.flywheel.backend.gl.GlCompat;
import dev.engine_room.flywheel.backend.gl.shader.GlProgram;
import dev.engine_room.flywheel.backend.gl.shader.ShaderType;
import dev.engine_room.flywheel.backend.glsl.GlslVersion;
import dev.engine_room.flywheel.backend.glsl.ShaderSources;
import dev.engine_room.flywheel.lib.util.ResourceUtil;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.List;

public class ClrwlOitPrograms
{
    public record OitCompositeShaderKey(int[] drawBuffers, int[] ranks, List<ClrwlOitAccumulateOverride> overrides, boolean isShadow)
    {
    }

    private static final ResourceLocation FULLSCREEN = ResourceUtil.rl("internal/fullscreen.vert");

    private static final Compile<OitCompositeShaderKey> COMPOSITE = new Compile<>();

    private final CompilationHarness<OitCompositeShaderKey> compositeHarness;

    public ClrwlOitPrograms(ShaderSources sources, ClrwlPrograms.Pipeline pipeline)
    {
        this.compositeHarness = createCompositePipeline(sources, pipeline);
    }

    private static CompilationHarness<OitCompositeShaderKey> createCompositePipeline(ShaderSources sources, ClrwlPrograms.Pipeline pipeline)
    {
        return COMPOSITE.program()
                .link(COMPOSITE.shader(GlCompat.MAX_GLSL_VERSION, ShaderType.VERTEX)
                        .nameMapper(k -> "oit_composite/fullscreen")
                        .onCompile((k, c) -> defineClrwlPass(k.isShadow(), c))
                        .withResource(FULLSCREEN))
                .link(COMPOSITE.shader(GlCompat.MAX_GLSL_VERSION, ShaderType.FRAGMENT)
                        .nameMapper(k -> "oit_composite/oit_composite" + (k.isShadow() ? "_shadow" : ""))
                        .onCompile((k, c) -> defineClrwlPass(k.isShadow(), c))
                        .onCompile(($, c) -> defineFmaFallback(c, pipeline.extensions()))
                        .with((k, src) -> new OitCompositeComponent(src, k.drawBuffers(), k.ranks(), k.overrides())))
                .postLink((k, p) ->
                {
                    p.bind();
                    p.setUniformBlockBinding(ClrwlUniforms.PASS_BLOCK_NAME, ClrwlUniforms.PASS_INDEX);

                    p.setSamplerBinding("_flw_depthRange", ClrwlSamplers.DEPTH_RANGE);

                    for (int i = 0; i < k.ranks().length; i++)
                    {
                        p.setSamplerBinding("clrwl_coefficients" + i, ClrwlSamplers.getCoefficient(i));
                    }

                    var drawBuffers = k.drawBuffers();

                    for (int i = 0; i < drawBuffers.length; i++)
                    {
                        p.setSamplerBinding("_clrwl_accumulate" + drawBuffers[i], ClrwlSamplers.getAccumulate(i));
                    }

                    GlProgram.unbind();
                })
                .harness("oit_composite", sources);
    }

    public static void defineClrwlPass(boolean isShadow, Compilation c)
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

    public static void defineFmaFallback(Compilation c, Collection<String> extensions)
    {
        if (GlCompat.MAX_GLSL_VERSION.compareTo(GlslVersion.V400) < 0 && !extensions.contains("GL_ARB_gpu_shader5"))
        {
            c.define("fma(a, b, c)", "((a) * (b) + (c))");
        }
    }

    public GlProgram getOitCompositeProgram(int[] drawBuffers, int[] ranks, List<ClrwlOitAccumulateOverride> overrides, boolean isShadow)
    {
        return compositeHarness.get(new OitCompositeShaderKey(drawBuffers, ranks, overrides, isShadow));
    }

    public void delete()
    {
        compositeHarness.delete();
    }
}
