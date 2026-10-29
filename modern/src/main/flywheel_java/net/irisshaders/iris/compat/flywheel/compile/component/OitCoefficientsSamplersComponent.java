package net.irisshaders.iris.compat.flywheel.compile.component;

import net.irisshaders.iris.compat.flywheel.Colorwheel;
import dev.engine_room.flywheel.backend.glsl.SourceComponent;
import dev.engine_room.flywheel.backend.glsl.generate.GlslBuilder;
import dev.engine_room.flywheel.backend.glsl.generate.GlslUniform;

import java.util.Collection;
import java.util.List;

public class OitCoefficientsSamplersComponent implements SourceComponent
{
    private final int coeffCount;

    public OitCoefficientsSamplersComponent(int coeffCount)
    {
        this.coeffCount = coeffCount;
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

        addSamplers(builder, coeffCount);

        return builder.build();
    }

    @Override
    public String name()
    {
        return Colorwheel.rl("oit_coefficients_samplers").toString();
    }

    public static void addSamplers(GlslBuilder builder, int coeffCount)
    {
        for (int i = 0; i < coeffCount; i++)
        {
            var uniform = new GlslUniform()
                    .type("sampler2DArray")
                    .name("clrwl_coefficients" + i);

            builder.add(uniform);
        }
    }
}
