package net.irisshaders.iris.compat.flywheel.compile.component;

import net.irisshaders.iris.compat.flywheel.Colorwheel;
import net.irisshaders.iris.compat.flywheel.compile.ClrwlPrograms;
import dev.engine_room.flywheel.backend.glsl.SourceComponent;
import dev.engine_room.flywheel.backend.glsl.generate.FnSignature;
import dev.engine_room.flywheel.backend.glsl.generate.GlslBlock;
import dev.engine_room.flywheel.backend.glsl.generate.GlslBuilder;
import dev.engine_room.flywheel.backend.glsl.generate.GlslStmt;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class OitCollectCoeffsComponent implements SourceComponent
{
    private final int[] ranks;
    private final Map<Integer, Integer> coeffFrag;
    private final Map<Integer, String> shaderOutputs;

    public OitCollectCoeffsComponent(int[] ranks, Map<Integer, Integer> coeffFrag, Map<Integer, String> shaderOutputs)
    {
        this.ranks = ranks;
        this.coeffFrag = coeffFrag;
        this.shaderOutputs = shaderOutputs;
    }

    @Override
    public Collection<? extends SourceComponent> included()
    {
        return List.of();
    }

    @Override
    public String source()
    {
        var builder = new GlslBuilder();
        var body = new GlslBlock();

        body.add(GlslStmt.raw("float linearDepth = _clrwl_linear_depth();"));
        body.add(GlslStmt.raw("vec2 depthRange = texelFetch(_flw_depthRange, ivec2(gl_FragCoord.xy), 0).rg;"));
        body.add(GlslStmt.raw("float delta = depthRange.x + depthRange.y;"));
        body.add(GlslStmt.raw("float flw_depth = (linearDepth + depthRange.x) / delta;"));
        body.add(GlslStmt.raw("float depth_adjustment = _clrwl_tented_blue_noise(flw_depth) * _flw_oitNoise;"));
        body.add(GlslStmt.raw(""));

        for (int i = 0; i < ranks.length; i++)
        {
            int rank = ranks[i];
            int frag = coeffFrag.get(i);
            int depth = 1 << (rank - 1);

            String name = "clrwl_coeffs" + i;
            String fragData = shaderOutputs.get(frag);
            String transmittance = "transmittance" + i;
            String adjusted_depth = "adjusted_depth" + i;

            body.add(GlslStmt.raw("float " + transmittance + " = 1.0 - " + fragData + ".a;"));
            body.add(GlslStmt.raw("float " + adjusted_depth + " = flw_depth;"));

            // Don't do the depth adjustment if this fragment is opaque.
            body.add(GlslStmt.raw("if (" + transmittance + " > 1e-5) { " + adjusted_depth + " -= depth_adjustment; } "));

            body.add(GlslStmt.raw("vec4[4] " + name + ";"));

            for (int d = 0; d < depth; d++)
            {
                body.add(GlslStmt.raw(name + "[" + d +"] = vec4(0.);"));
            }

            body.add(GlslStmt.raw("_clrwl_add_transmittance(" + name + ", " + transmittance + ", " + adjusted_depth + ", " + rank + ");"));

            for (int d = 0; d < depth; d++)
            {
                // clrwl_coeffs12
                String outName = name + d;

                body.add(GlslStmt.raw(outName + " = " + name + "[" + d + "];"));
            }
        }

        builder.function()
                .signature(FnSignature.create()
                        .returnType("void")
                        .name(ClrwlPrograms.CLRWL_POST_FRAGMENT_FCT)
                        .build())
                .body(body);

        return builder.build();
    }

    @Override
    public String name()
    {
        return Colorwheel.rl("oit_collect_coeffs").toString();
    }
}
