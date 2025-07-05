package org.embeddedt.embeddium.impl.render.chunk.sprite;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import org.embeddedt.embeddium.impl.render.chunk.data.MinecraftBuiltRenderSectionData;
import org.embeddedt.embeddium.impl.render.chunk.lists.ChunkRenderList;
import org.embeddedt.embeddium.impl.render.chunk.lists.SectionTicker;

import java.util.List;
import java.util.function.Consumer;

public class GenericSectionSpriteTicker<T> implements SectionTicker {
    private volatile ReferenceOpenHashSet<T> sprites = new ReferenceOpenHashSet<>();

    private final Consumer<T> markActive;

    public GenericSectionSpriteTicker(Consumer<T> markActive) {
        this.markActive = markActive;
    }

    @Override
    public void tickVisibleRenders() {
        this.sprites.forEach(this.markActive);
    }

    @Override
    public void onRenderListUpdated(List<ChunkRenderList> renderLists) {
        var spriteSet = new ReferenceOpenHashSet<T>(this.sprites.size());

        for (ChunkRenderList renderList : renderLists) {
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

                if (!(context instanceof MinecraftBuiltRenderSectionData<?, ?> mcData)) {
                    continue;
                }

                //noinspection unchecked
                var sprites = (List<T>) mcData.animatedSprites;

                // The iterator allocation is very expensive here for large render distances.
                //noinspection ForLoopReplaceableByForEach
                for (int i = 0; i < sprites.size(); i++) {
                    //noinspection UseBulkOperation
                    spriteSet.add(sprites.get(i));
                }
            }
        }

        this.sprites = spriteSet;
    }
}
