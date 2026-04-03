package org.embeddedt.embeddium.impl.loader.common;

public enum Distribution {
    CLIENT,
    SERVER;

    public boolean isClient() {
        return this == CLIENT;
    }
}
