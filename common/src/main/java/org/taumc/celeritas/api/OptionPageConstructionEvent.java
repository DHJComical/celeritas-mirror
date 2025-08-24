package org.taumc.celeritas.api;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.taumc.celeritas.api.options.structure.OptionGroup;
import org.taumc.celeritas.api.eventbus.EmbeddiumEvent;
import org.taumc.celeritas.api.eventbus.EventHandlerRegistrar;
import org.taumc.celeritas.api.options.OptionIdentifier;
import org.embeddedt.embeddium.impl.gui.framework.TextComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Fired when an option page is created, to allow adding additional {@link OptionGroup} entries at the end of the page.
 */
@RequiredArgsConstructor
@Getter
public class OptionPageConstructionEvent extends EmbeddiumEvent {
    public static final EventHandlerRegistrar<OptionPageConstructionEvent> BUS = new EventHandlerRegistrar<>();

    /**
     * Returns the ID of the option group.
     */
    private final OptionIdentifier<Void> id;
    /**
     * The translation key for the group's name.
     */
    private final TextComponent translationKey;

    private final List<OptionGroup> additionalGroups = new ArrayList<>();

    /**
     * Add a new option group to the end of this page. The group will be inserted at the end, after any
     * existing groups.
     */
    public void addGroup(OptionGroup group) {
        this.additionalGroups.add(group);
    }

    public List<OptionGroup> getAdditionalGroups() {
        return Collections.unmodifiableList(this.additionalGroups);
    }
}
