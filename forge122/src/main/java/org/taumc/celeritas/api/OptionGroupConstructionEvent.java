package org.taumc.celeritas.api;

import java.util.List;
import org.taumc.celeritas.api.eventbus.CeleritasEvent;
import org.taumc.celeritas.api.eventbus.EventHandlerRegistrar;
import org.taumc.celeritas.api.options.OptionIdentifier;
import org.taumc.celeritas.api.options.structure.Option;

public class OptionGroupConstructionEvent extends CeleritasEvent {
    public static final EventHandlerRegistrar<OptionGroupConstructionEvent> BUS = new EventHandlerRegistrar<>();

    private final OptionIdentifier<Void> id;
    private final List<Option<?>> options;

    public OptionGroupConstructionEvent(OptionIdentifier<Void> id, List<Option<?>> options) {
        this.id = id;
        this.options = options;
    }

    public List<Option<?>> getOptions() {
        return this.options;
    }

    public OptionIdentifier<Void> getId() {
        return this.id;
    }
}
