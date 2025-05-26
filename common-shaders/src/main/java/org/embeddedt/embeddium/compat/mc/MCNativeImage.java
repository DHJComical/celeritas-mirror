package org.embeddedt.embeddium.compat.mc;

import java.io.IOException;
import java.io.InputStream;

public interface MCNativeImage {
    int getWidth();
    int getHeight();
    void setPixelRGBA(int x, int y, int color);
    int getPixelRGBA(int x, int y);
//    long getPointer();

    void upload(int level, int xOffset, int yOffset, int unpackSkipPixels, int unpackSkipRows, int width, int height, boolean blur, boolean clamp, boolean mipmap, boolean autoClose);
}
