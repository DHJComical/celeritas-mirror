package org.taumc.celeritas.api.options.control;

import org.taumc.celeritas.api.options.structure.Option;

public interface OptionControlElement<T> {
    Option<T> getOption();
}