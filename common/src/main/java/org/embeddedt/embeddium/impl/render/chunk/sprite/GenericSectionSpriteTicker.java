package org.embeddedt.embeddium.impl.render.chunk.sprite;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import org.embeddedt.embeddium.impl.render.chunk.data.MinecraftBuiltRenderSectionData;
import org.embeddedt.embeddium.impl.render.chunk.lists.ChunkRenderList;
import org.embeddedt.embeddium.impl.render.chunk.lists.SectionTicker;
import org.embeddedt.embeddium.impl.render.chunk.lists.SortedRenderLists;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

public class GenericSectionSpriteTicker<T> implements SectionTicker {
    private final ReferenceOpenHashSet<T> sprites = new ReferenceOpenHashSet<>();

    private final Consumer<T> markActive;

    public GenericSectionSpriteTicker(Consumer<T> markActive) {
        this.markActive = markActive;
    }

    @Override
    public void tickVisibleRenders() {
        this.sprites.forEach(this.markActive);
    }

    @Override
    public void onRenderListUpdated(SortedRenderLists renderLists) {
        this.sprites.clear();

        Iterator<ChunkRenderList> it = renderLists.iterator();

        while (it.hasNext()) {
            ChunkRenderList renderList = it.next();

            var region = renderList.getRegion();
            var iterator = renderList.sectionsWithSpritesIterator();

            if (iterator == null) {
                continue;
            }

            while (iterator.hasNext()) {
                var section = region.getSection(iterator.nextByteAsInt());

                if (section == null) {
                    continue;
                }

                var context = section.getBuiltContext();

                if (!(context instanceof MinecraftBuiltRenderSectionData<?,?> mcData)) {
                    continue;
                }

                //noinspection unchecked
                var sprites = (List<T>)mcData.animatedSprites;

                // The iterator allocation is very expensive here for large render distances.
                //noinspection ForLoopReplaceableByForEach
                for (int i = 0; i < sprites.size(); i++) {
                    //noinspection UseBulkOperation
                    this.sprites.add(sprites.get(i));
                }
            }
        }
    }
}
