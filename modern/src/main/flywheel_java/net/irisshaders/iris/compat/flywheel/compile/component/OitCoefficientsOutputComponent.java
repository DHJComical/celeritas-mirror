package net.irisshaders.iris.compat.flywheel.compile.component;

import net.irisshaders.iris.compat.flywheel.Colorwheel;
import net.irisshaders.iris.compat.flywheel.compile.glsl.GlslFragmentOutput;
import dev.engine_room.flywheel.backend.glsl.SourceComponent;
import dev.engine_room.flywheel.backend.glsl.generate.GlslBuilder;

import java.util.Collection;
import java.util.List;

public class OitCoefficientsOutputComponent implements SourceComponent
{
    private final int[] ranks;

    public OitCoefficientsOutputComponent(int[] ranks)
    {
        this.ranks = ranks;
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
        int idx = 0;

        for (int i = 0; i < ranks.length; i++)
        {
            int rank = ranks[i];
            int depth = 1 << (rank - 1);

            for (int d = 0; d < depth; d++)
            {
                var out = new GlslFragmentOutput()
                        .binding(idx)
                        .type("vec4")
                        .name(("clrwl_coeffs" + i) + d);

                builder.add(out);
                idx += 1;
            }
        }

        return builder.build();
    }

    @Override
    public String name()
    {
        return Colorwheel.rl("oit_coefficients_output").toString();
    }
}
