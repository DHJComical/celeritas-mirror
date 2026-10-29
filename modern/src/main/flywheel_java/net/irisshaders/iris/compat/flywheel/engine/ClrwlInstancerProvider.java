package net.irisshaders.iris.compat.flywheel.engine;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.api.instance.Instancer;
import dev.engine_room.flywheel.api.instance.InstancerProvider;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.backend.engine.embed.GlobalEnvironment;

public record ClrwlInstancerProvider(ClrwlEngine engine, ClrwlInstanceVisual visual) implements InstancerProvider
{
    @Override
    public <I extends Instance> Instancer<I> instancer(InstanceType<I> type, Model model, int bias)
    {
        return engine.instancer(visual, GlobalEnvironment.INSTANCE, type, model, bias);
    }
}
