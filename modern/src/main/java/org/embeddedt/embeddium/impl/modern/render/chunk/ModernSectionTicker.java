package org.embeddedt.embeddium.impl.modern.render.chunk;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.embeddedt.embeddium.api.render.texture.SpriteUtil;
import org.embeddedt.embeddium.impl.render.chunk.lists.ChunkRenderList;
import org.embeddedt.embeddium.impl.render.chunk.lists.SectionTicker;
import org.embeddedt.embeddium.impl.render.chunk.lists.SortedRenderLists;

import java.util.Iterator;
import java.util.List;

public class ModernSectionTicker implements SectionTicker {
    private final ReferenceOpenHashSet<TextureAtlasSprite> sprites = new ReferenceOpenHashSet<>();

    @Override
    public void tickVisibleRenders() {
        this.sprites.forEach(SpriteUtil::markSpriteActive);
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

                var sprites = (List<TextureAtlasSprite>)section.getContextOrDefault(ModernRenderSectionBuiltInfo.ANIMATED_SPRITES);

                this.sprites.addAll(sprites);
            }
        }
    }
}
