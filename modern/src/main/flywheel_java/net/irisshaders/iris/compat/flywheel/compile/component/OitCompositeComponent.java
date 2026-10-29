package net.irisshaders.iris.compat.flywheel.compile.component;

import net.irisshaders.iris.compat.flywheel.Colorwheel;
import net.irisshaders.iris.compat.flywheel.compile.glsl.GlslFragmentOutput;
import net.irisshaders.iris.compat.flywheel.engine.ClrwlOitAccumulateOverride;
import net.irisshaders.iris.compat.flywheel.util.Utils;
import dev.engine_room.flywheel.backend.glsl.ShaderSources;
import dev.engine_room.flywheel.backend.glsl.SourceComponent;
import dev.engine_room.flywheel.backend.glsl.generate.FnSignature;
import dev.engine_room.flywheel.backend.glsl.generate.GlslBlock;
import dev.engine_room.flywheel.backend.glsl.generate.GlslBuilder;
import dev.engine_room.flywheel.backend.glsl.generate.GlslUniform;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class OitCompositeComponent implements SourceComponent
{
    private final ShaderSources sources;
    private final int[] drawBuffers;
    private final int[] ranks;
    private final List<ClrwlOitAccumulateOverride> overrides;

    public OitCompositeComponent(ShaderSources sources, int[] drawBuffers, int[] ranks, List<ClrwlOitAccumulateOverride> overrides)
    {
        this.sources = sources;
        this.drawBuffers = drawBuffers;
        this.ranks = ranks;
        this.overrides = overrides;
    }

    @Override
    public Collection<? extends SourceComponent> included()
    {
        return List.of(
                sources.get(Colorwheel.rl("internal/oit/wavelet.glsl")),
                sources.get(Colorwheel.rl("internal/uniforms.glsl")),
                sources.get(Colorwheel.rl("internal/depth.glsl"))
            );
    }

    @Override
    public String source()
    {
        var builder = new GlslBuilder();

        OitCoefficientsSamplersComponent.addSamplers(builder, ranks.length);
        addAccumulateSamplers(builder, drawBuffers);
        builder.uniform().type("sampler2D").name("_flw_depthRange");

        addOutputs(builder, drawBuffers.length);

        var body = new GlslBlock();

        body.raw("float minDepth = -texelFetch(_flw_depthRange, ivec2(gl_FragCoord.xy), 0).r;");

        // Required otherwise depth buffer is corrupted
        body.raw("if (minDepth == _flw_cullData.zfar) { discard; }");

        body.raw("gl_FragDepth = _clrwl_delinearize_depth(minDepth, _flw_cullData.znear, _flw_cullData.zfar);");

        for (int i = 0; i < ranks.length; i++)
        {
            var rank = ranks[i];

            var name = "total_transmittance" + i;
            var coeffName = "clrwl_coefficients" + i;

            body.raw("float " + name + " = _clrwl_total_transmittance(" + coeffName + ", " + rank + ");");
        }

        for (int i = 0; i < drawBuffers.length; i++)
        {
            var drawBuffer = drawBuffers[i];
            Optional<Integer> coeffId = Utils.findFirst(overrides, e -> e.drawBuffer() == drawBuffer)
                .flatMap(ClrwlOitAccumulateOverride::coefficientId);

            var accumulate = "_clrwl_accumulate" + drawBuffer;
            var out = "frag" + i;

            if (coeffId.isPresent())
            {
                var texelName = "texelTranslucent" + drawBuffer;
                var totalName = "total_transmittance" + coeffId.get();

                body.raw("vec4 " + texelName + " = texelFetch(" + accumulate + ", ivec2(gl_FragCoord.xy), 0);");
                body.raw("if (" + texelName + ".a >= 1e-5) {");
                body.raw("    " + out + " = vec4(" + texelName + ".rgb / " + texelName + ".a, 1. - " + totalName + ");");
                body.raw("} else {");
                body.raw("    " + out + " = vec4(0.0);");
                body.raw("}");
            }
            else // Frontmost
            {
                body.raw(out + " = texelFetch(" + accumulate + ", ivec2(gl_FragCoord.xy), 0);");
            }
        }

        builder.function()
                .signature(FnSignature.create().returnType("void").name("main").build())
                .body(body);

        return builder.build();
    }

    @Override
    public String name()
    {
        return Colorwheel.rl("oit_composite").toString();
    }

    public static void addOutputs(GlslBuilder builder, int targetCnt)
    {
        for (int i = 0; i < targetCnt; i++)
        {
            var out = new GlslFragmentOutput()
                    .binding(i)
                    .type("vec4")
                    .name("frag" + i);

            builder.add(out);
        }
    }

    public static void addAccumulateSamplers(GlslBuilder builder, int[] drawBuffers)
    {
        for (int drawBuffer : drawBuffers)
        {
            var uniform = new GlslUniform()
                    .type("sampler2D")
                    .name("_clrwl_accumulate" + drawBuffer);

            builder.add(uniform);
        }
    }
}
