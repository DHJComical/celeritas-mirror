package org.embeddedt.embeddium.api.options.structure;

import org.embeddedt.embeddium.api.options.control.Control;
import org.embeddedt.embeddium.api.options.OptionIdentifier;
import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public interface Option<T> {
    @Nullable
    default OptionIdentifier<T> getId() {
        return null;
    }

    TextComponent getName();

    TextComponent getTooltip();

    OptionImpact getImpact();

    Control<T> getControl();

    T getValue();

    void setValue(T value);

    void reset();

    OptionStorage<?> getStorage();

    boolean isAvailable();

    boolean hasChanged();

    void applyChanges();

    Collection<OptionFlag> getFlags();
}
