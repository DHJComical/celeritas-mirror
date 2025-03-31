package org.embeddedt.embeddium.compat.mc;

public interface MCAbstractTexture {
    int getId();
    int releaseId();
    void bind();
    void close();
}
