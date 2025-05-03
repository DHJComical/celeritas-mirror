package org.embeddedt.embeddium.compat.mc;

public interface MCAbstractTexture {
    int getId();
    void releaseId();
    void bind();
    void close();
}
