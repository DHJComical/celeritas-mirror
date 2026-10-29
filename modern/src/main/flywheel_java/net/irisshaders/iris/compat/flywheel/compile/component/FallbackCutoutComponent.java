package net.irisshaders.iris.compat.flywheel.compile.component;

import net.irisshaders.iris.compat.flywheel.Colorwheel;
import net.irisshaders.iris.compat.flywheel.compile.ClrwlPrograms;
import dev.engine_room.flywheel.backend.glsl.SourceComponent;
import dev.engine_room.flywheel.backend.glsl.generate.FnSignature;
import dev.engine_room.flywheel.backend.glsl.generate.GlslBlock;
import dev.engine_room.flywheel.backend.glsl.generate.GlslBuilder;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class FallbackCutoutComponent implements SourceComponent
{
    private final Map<Integer, String> shaderOutputs;

    public FallbackCutoutComponent(Map<Integer, String> shaderOutputs)
    {
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
        // output 0 is assumed to be diffuse, too bad if it isn't
        var outName = shaderOutputs.get(0);

        var builder = new GlslBuilder();
        var body = new GlslBlock();

        if (outName != null)
        {
            body.raw("clrwl_computeDiscard(" + outName + ");");
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
        return Colorwheel.rl("fallback_cutout").toString();
    }
}
