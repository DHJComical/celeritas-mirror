package org.embeddedt.embeddium.impl.render.chunk.compile;

import org.embeddedt.embeddium.impl.render.chunk.RenderSection;

public abstract class ChunkTaskOutput {
    public final RenderSection render;
    public final int buildTime;

    protected ChunkTaskOutput(RenderSection render, int buildTime) {
        this.render = render;
        this.buildTime = buildTime;
    }

    public void delete() {

    }
}
