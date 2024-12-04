package org.embeddedt.embeddium.api.options.structure;

import com.google.common.collect.ImmutableList;
import org.embeddedt.embeddium.impl.Celeritas;
import net.minecraft.network.chat.Component;
//? if >=1.19 {
import net.minecraft.network.chat.contents.TranslatableContents;
//?} else
/*import net.minecraft.network.chat.TranslatableComponent;*/
import org.embeddedt.embeddium.api.OptionPageConstructionEvent;
import org.embeddedt.embeddium.api.options.OptionIdentifier;

import java.util.List;

public class OptionPage {
    public static final OptionIdentifier<Void> DEFAULT_ID = OptionIdentifier.create(Celeritas.MODID, "empty");

    private final OptionIdentifier<Void> id;
    private final Component name;
    private final ImmutableList<OptionGroup> groups;
    private final ImmutableList<Option<?>> options;

    private static String findKey(Component name) {
        //? if >=1.19 {
        if(name.getContents() instanceof TranslatableContents translatableContents) {
            String key = translatableContents.getKey();
            if (name.getSiblings().isEmpty()) {
                return key;
            }
        }
        //?} else {
        /*if(name instanceof TranslatableComponent component) {
            if (component.getSiblings().isEmpty()) {
                return component.getKey();
            }
        }
        *///?}
        return null;
    }

    private static OptionIdentifier<Void> tryMakeId(Component name) {
        OptionIdentifier<Void> id = null;
        String key = findKey(name);
        if (key != null) {
            // Detect our own tabs
            id = switch(key) {
                case "stat.generalButton" -> StandardOptions.Pages.GENERAL;
                case "sodium.options.pages.quality" -> StandardOptions.Pages.QUALITY;
                case "sodium.options.pages.advanced" -> StandardOptions.Pages.ADVANCED;
                case "sodium.options.pages.performance" -> StandardOptions.Pages.PERFORMANCE;
                default -> null;
            };
        }
        if(id != null) {
            return id;
        } else {
            throw new IllegalStateException("ID must be provided for option page");
        }
    }

    @Deprecated
    public OptionPage(Component name, ImmutableList<OptionGroup> groups) {
        this(tryMakeId(name), name, groups);
    }

    public OptionPage(OptionIdentifier<Void> id, Component name, ImmutableList<OptionGroup> groups) {
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

    public Component getName() {
        return this.name;
    }

}
