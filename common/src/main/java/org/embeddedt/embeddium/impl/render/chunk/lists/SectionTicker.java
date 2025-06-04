package org.embeddedt.embeddium.impl.render.chunk.lists;

import java.util.List;

public interface SectionTicker {
    void tickVisibleRenders();
    void onRenderListUpdated(List<ChunkRenderList> renderLists);
}
