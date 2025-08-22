package org.embeddedt.embeddium.api.options.structure;

import com.google.common.collect.ImmutableList;
import org.embeddedt.embeddium.api.OptionPageConstructionEvent;
import org.embeddedt.embeddium.api.options.OptionIdentifier;
import org.embeddedt.embeddium.impl.gui.framework.TextComponent;

import java.util.List;

public class OptionPage {
    private final OptionIdentifier<Void> id;
    private final TextComponent name;
    private final ImmutableList<OptionGroup> groups;
    private final ImmutableList<Option<?>> options;

    public OptionPage(OptionIdentifier<Void> id, TextComponent name, ImmutableList<OptionGroup> groups) {
        this.id = id;
        this.name = name;
        this.groups = collectExtraGroups(groups);

        ImmutableList.Builder<Option<?>> builder = ImmutableList.builder();

        for (OptionGroup group : this.groups) {
            builder.addAll(group.getOptions());
        }

        this.options = builder.build();
    }

    private ImmutableList<OptionGroup> collectExtraGroups(ImmutableList<OptionGroup> groups) {
        OptionPageConstructionEvent event = new OptionPageConstructionEvent(this.id, this.name);
        OptionPageConstructionEvent.BUS.post(event);
        List<OptionGroup> extraGroups = event.getAdditionalGroups();
        return extraGroups.isEmpty() ? groups : ImmutableList.<OptionGroup>builder().addAll(groups).addAll(extraGroups).build();
    }

    public OptionIdentifier<Void> getId() {
        return id;
    }

    public ImmutableList<OptionGroup> getGroups() {
        return this.groups;
    }

    public ImmutableList<Option<?>> getOptions() {
        return this.options;
    }

    public TextComponent getName() {
        return this.name;
    }
}
