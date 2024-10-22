package org.embeddedt.embeddium.impl.gui.options.storage;

public interface OptionStorage<T> {
    T getData();

    void save();
}
