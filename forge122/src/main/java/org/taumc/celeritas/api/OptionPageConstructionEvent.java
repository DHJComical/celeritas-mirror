package org.taumc.celeritas.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.util.text.ITextComponent;
import org.taumc.celeritas.api.eventbus.CeleritasEvent;
import org.taumc.celeritas.api.eventbus.EventHandlerRegistrar;
import org.taumc.celeritas.api.options.OptionIdentifier;
import org.taumc.celeritas.api.options.structure.OptionGroup;

public class OptionPageConstructionEvent extends CeleritasEvent {
    public static final EventHandlerRegistrar<OptionPageConstructionEvent> BUS = new EventHandlerRegistrar<>();

    private final OptionIdentifier<Void> id;
    private final ITextComponent name;
    private final List<OptionGroup> additionalGroups = new ArrayList<>();

    public OptionPageConstructionEvent(OptionIdentifier<Void> id, ITextComponent name) {
        this.id = id;
        this.name = name;
    }

    public OptionIdentifier<Void> getId() {
        return this.id;
    }

    public ITextComponent getName() {
        return this.name;
    }

    public void addGroup(OptionGroup group) {
        this.additionalGroups.add(group);
    }

    public List<OptionGroup> getAdditionalGroups() {
        return Collections.unmodifiableList(this.additionalGroups);
    }
}
