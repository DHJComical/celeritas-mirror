package org.embeddedt.embeddium.api.options.structure;

import java.util.Set;

public interface OptionStorage<T> {
    T getData();

    default void save() {

    }

    default void save(Set<OptionFlag> flags) {
        save();
    }
}
