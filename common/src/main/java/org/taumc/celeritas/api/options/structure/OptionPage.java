package org.taumc.celeritas.api.options.structure;

import org.taumc.celeritas.api.OptionPageConstructionEvent;
import org.taumc.celeritas.api.options.OptionIdentifier;
import org.embeddedt.embeddium.impl.gui.framework.TextComponent;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public class OptionPage {
    private final OptionIdentifier<Void> id;
    private final TextComponent name;
    private final List<OptionGroup> groups;
    private final List<Option<?>> options;

    public OptionPage(OptionIdentifier<Void> id, TextComponent name, List<OptionGroup> groups) {
        this.id = id;
        this.name = name;
        this.groups = collectExtraGroups(groups);

        this.options = this.groups.stream().flatMap(g -> g.getOptions().stream()).toList();
    }

    private List<OptionGroup> collectExtraGroups(List<OptionGroup> groups) {
        OptionPageConstructionEvent event = new OptionPageConstructionEvent(this.id, this.name);
        OptionPageConstructionEvent.BUS.post(event);
        List<OptionGroup> extraGroups = event.getAdditionalGroups();
        return extraGroups.isEmpty() ? groups : Stream.of(groups.stream(), extraGroups.stream()).flatMap(Function.identity()).toList();
    }

    public OptionIdentifier<Void> getId() {
        return id;
    }

    public List<OptionGroup> getGroups() {
        return this.groups;
    }

    public List<Option<?>> getOptions() {
        return this.options;
    }

    public TextComponent getName() {
        return this.name;
    }
}
