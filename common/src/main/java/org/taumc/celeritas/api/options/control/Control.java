package org.taumc.celeritas.api.options.control;

import org.taumc.celeritas.api.options.structure.Option;
import org.embeddedt.embeddium.impl.util.Dim2i;

public interface Control<T> {
    Option<T> getOption();

    ControlElement<T> createElement(Dim2i dim);

    int getMaxWidth();
}
