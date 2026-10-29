package net.irisshaders.iris.compat.flywheel.compile.glsl;

import dev.engine_room.flywheel.backend.glsl.generate.GlslBuilder;

public class GlslFragmentOutput implements GlslBuilder.Declaration
{
    private int binding;
    private String type;
    private String name;

    public GlslFragmentOutput() {}

    public GlslFragmentOutput binding(int binding)
    {
        this.binding = binding;
        return this;
    }

    public GlslFragmentOutput type(String type)
    {
        this.type = type;
        return this;
    }

    public GlslFragmentOutput name(String name)
    {
        this.name = name;
        return this;
    }

    public String prettyPrint()
    {
        return "layout(location = " + this.binding + ") out " + this.type + " " + this.name + ";";
    }
}
