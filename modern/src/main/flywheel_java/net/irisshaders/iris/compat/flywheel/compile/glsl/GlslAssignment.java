package net.irisshaders.iris.compat.flywheel.compile.glsl;

import dev.engine_room.flywheel.backend.glsl.generate.GlslBuilder;

public class GlslAssignment implements GlslBuilder.Declaration
{
    private String type;
    private String name;

    public GlslAssignment() {}

    public GlslAssignment type(String type)
    {
        this.type = type;
        return this;
    }

    public GlslAssignment name(String name)
    {
        this.name = name;
        return this;
    }

    public String prettyPrint()
    {
        return this.type + " " + this.name + ";";
    }
}
