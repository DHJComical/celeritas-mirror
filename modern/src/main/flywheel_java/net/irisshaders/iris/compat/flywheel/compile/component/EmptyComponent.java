package net.irisshaders.iris.compat.flywheel.compile.component;

import net.irisshaders.iris.compat.flywheel.Colorwheel;
import dev.engine_room.flywheel.backend.glsl.SourceComponent;

import java.util.Collection;
import java.util.List;

public class EmptyComponent implements SourceComponent
{
    @Override
    public Collection<? extends SourceComponent> included()
    {
        return List.of();
    }

    @Override
    public String source()
    {
        return "";
    }

    @Override
    public String name()
    {
        return Colorwheel.rl("empty").getPath();
    }
}
