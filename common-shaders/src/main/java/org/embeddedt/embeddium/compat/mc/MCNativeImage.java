package org.embeddedt.embeddium.compat.mc;

public interface MCNativeImage {
    int getWidth();
    int getHeight();
    void setPixelRGBA(int x, int y, int color);
    int getPixelRGBA(int x, int y);
    long getPointer();
}
