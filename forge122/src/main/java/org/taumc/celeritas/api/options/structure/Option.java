package org.taumc.celeritas.api.options.structure;

import java.util.Collection;
import net.minecraft.util.text.ITextComponent;
import org.jetbrains.annotations.Nullable;
import org.taumc.celeritas.api.options.OptionIdentifier;
import org.taumc.celeritas.api.options.control.Control;

public interface Option<T> {
    @Nullable
    default OptionIdentifier<T> getId() {
        return null;
    }

    ITextComponent getName();

    ITextComponent getTooltip();

    OptionImpact getImpact();

    Control<T> getControl();

    T getValue();

    void setValue(T var1);

    void reset();

    OptionStorage<?> getStorage();

    boolean isAvailable();

    boolean hasChanged();

    void applyChanges();

    Collection<OptionFlag> getFlags();
}
