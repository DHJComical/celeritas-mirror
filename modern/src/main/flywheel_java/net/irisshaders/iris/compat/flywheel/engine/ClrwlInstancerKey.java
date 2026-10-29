package net.irisshaders.iris.compat.flywheel.engine;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.backend.engine.embed.Environment;

public record ClrwlInstancerKey<I extends Instance>(ClrwlInstanceVisual visual, Environment environment, InstanceType<I> type, Model model, int bias)
{
    public ClrwlInstancerKey(ClrwlInstanceVisual visual, Environment environment, InstanceType<I> type, Model model, int bias)
    {
        this.visual = visual;
        this.environment = environment;
        this.type = type;
        this.model = model;
        this.bias = bias;
    }

    public ClrwlInstanceVisual visual() { return this.visual; }

    public Environment environment() {
        return this.environment;
    }

    public InstanceType<I> type() {
        return this.type;
    }

    public Model model() {
        return this.model;
    }

    public int bias() {
        return this.bias;
    }
}
