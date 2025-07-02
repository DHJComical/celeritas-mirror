package org.embeddedt.embeddium.compat.mc;

public interface MCResourceLocation {
    String getPath();
    String getNamespace();
    String toDebugFileName();
}
