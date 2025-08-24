package org.embeddedt.embeddium.api.options.control;

import org.embeddedt.embeddium.api.options.structure.Option;
import org.embeddedt.embeddium.impl.util.Dim2i;

public interface Control<T> {
    Option<T> getOption();

    ControlElement<T> createElement(Dim2i dim);

    int getMaxWidth();
}
