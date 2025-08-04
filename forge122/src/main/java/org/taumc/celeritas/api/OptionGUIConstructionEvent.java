package org.taumc.celeritas.api;

import java.util.List;
import org.taumc.celeritas.api.eventbus.CeleritasEvent;
import org.taumc.celeritas.api.eventbus.EventHandlerRegistrar;
import org.taumc.celeritas.api.options.structure.OptionPage;

public class OptionGUIConstructionEvent extends CeleritasEvent {
    public static final EventHandlerRegistrar<OptionGUIConstructionEvent> BUS = new EventHandlerRegistrar<>();

    private final List<OptionPage> pages;

    public OptionGUIConstructionEvent(List<OptionPage> pages) {
        this.pages = pages;
    }

    public List<OptionPage> getPages() {
        return this.pages;
    }

    public void addPage(OptionPage page) {
        this.pages.add(page);
    }
}
