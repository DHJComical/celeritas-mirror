package net.irisshaders.iris.compat.flywheel.compile.component;

import net.irisshaders.iris.compat.flywheel.Colorwheel;
import dev.engine_room.flywheel.backend.glsl.SourceComponent;

import java.util.Collection;
import java.util.Collections;

public class NoopFunctionComponent implements SourceComponent
{
    private final String name;

    public NoopFunctionComponent(String name)
    {
        this.name = name;
    }

    @Override
    public Collection<? extends SourceComponent> included()
    {
        return Collections.emptyList();
    }

    @Override
    public String source()
    {
        return "void " + name + "() {}";
    }

    @Override
    public String name()
    {
        return Colorwheel.rl(name + "_noop").toString();
    }
}
