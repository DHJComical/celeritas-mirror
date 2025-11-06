package org.embeddedt.embeddium.impl.gl.array;

import org.embeddedt.embeddium.impl.gl.GlObject;
import org.embeddedt.embeddium.impl.gl.util.VAOUtil;

/**
 * Provides Vertex Array functionality on supported platforms.
 */
public class GlVertexArray extends GlObject {
    public static final int NULL_ARRAY_ID = 0;

    public GlVertexArray() {
        this.setHandle(VAOUtil.glGenVertexArrays());
    }

    @Override
    protected void destroyInternal() {
        VAOUtil.glDeleteVertexArrays(this.handle());
    }
}
