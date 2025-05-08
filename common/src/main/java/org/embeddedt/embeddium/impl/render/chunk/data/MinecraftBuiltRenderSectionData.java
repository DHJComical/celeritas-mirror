package org.embeddedt.embeddium.impl.render.chunk.data;

import com.google.common.collect.Iterators;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.embeddedt.embeddium.impl.render.chunk.RenderSection;
import org.embeddedt.embeddium.impl.render.chunk.lists.ChunkRenderList;
import org.embeddedt.embeddium.impl.render.chunk.lists.RenderVisualsService;
import org.embeddedt.embeddium.impl.render.chunk.lists.SortedRenderLists;

import java.util.*;
import java.util.function.Consumer;

public class MinecraftBuiltRenderSectionData<SPRITE, BLOCKENTITY> extends BuiltRenderSectionData {
    public Collection<SPRITE> animatedSprites = new ObjectOpenHashSet<>();
    public List<BLOCKENTITY> culledBlockEntities = new ArrayList<>();
    public List<BLOCKENTITY> globalBlockEntities = new ArrayList<>();

    @Override
    public void bake() {
        super.bake();
        animatedSprites = List.copyOf(animatedSprites);
        culledBlockEntities = List.copyOf(culledBlockEntities);
        globalBlockEntities = List.copyOf(globalBlockEntities);
    }

    @Override
    public int getVisualBitmaskForSection() {
        int flags = super.getVisualBitmaskForSection();
        if (!animatedSprites.isEmpty()) {
            flags |= (1 << RenderVisualsService.HAS_SPRITES);
        }
        if (!culledBlockEntities.isEmpty() || !globalBlockEntities.isEmpty()) {
            flags |= (1 << RenderVisualsService.HAS_BLOCK_ENTITIES);
        }
        return flags;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        MinecraftBuiltRenderSectionData<?, ?> that = (MinecraftBuiltRenderSectionData<?, ?>) o;
        return Objects.equals(animatedSprites, that.animatedSprites) && Objects.equals(culledBlockEntities, that.culledBlockEntities) && Objects.equals(globalBlockEntities, that.globalBlockEntities);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), animatedSprites, culledBlockEntities, globalBlockEntities);
    }

    @SuppressWarnings("unchecked")
    public static <BLOCKENTITY> Iterator<BLOCKENTITY> generateBlockEntityIterator(SortedRenderLists renderLists, Collection<RenderSection> globalSections) {
        List<Iterator<BLOCKENTITY>> iterators = new ArrayList<>();

        Iterator<ChunkRenderList> renderListIterator = renderLists.iterator();

        while (renderListIterator.hasNext()) {
            var renderList = renderListIterator.next();

            var renderRegion = renderList.getRegion();
            var renderSectionIterator = renderList.sectionsWithEntitiesIterator();

            if (renderSectionIterator == null) {
                continue;
            }

            while (renderSectionIterator.hasNext()) {
                var renderSectionId = renderSectionIterator.nextByteAsInt();
                var renderSection = renderRegion.getSection(renderSectionId);

                if (renderSection == null) {
                    continue;
                }

                var context = renderSection.getBuiltContext();

                if (context instanceof MinecraftBuiltRenderSectionData<?,?> mcData) {
                    iterators.add((Iterator<BLOCKENTITY>)mcData.culledBlockEntities.iterator());
                }
            }
        }

        for (var renderSection : globalSections) {
            var context = renderSection.getBuiltContext();

            if (context instanceof MinecraftBuiltRenderSectionData<?,?> mcData) {
                iterators.add((Iterator<BLOCKENTITY>)mcData.globalBlockEntities.iterator());
            }
        }

        if(iterators.isEmpty()) {
            return Collections.emptyIterator();
        } else {
            return Iterators.concat(iterators.iterator());
        }
    }

    @SuppressWarnings("unchecked")
    public static <BLOCKENTITY> void forEachBlockEntity(Consumer<BLOCKENTITY> consumer, SortedRenderLists renderLists, Collection<RenderSection> globalSections) {
        Iterator<ChunkRenderList> renderListIterator = renderLists.iterator();

        while (renderListIterator.hasNext()) {
            var renderList = renderListIterator.next();

            var renderRegion = renderList.getRegion();
            var renderSectionIterator = renderList.sectionsWithEntitiesIterator();

            if (renderSectionIterator == null) {
                continue;
            }

            while (renderSectionIterator.hasNext()) {
                var renderSectionId = renderSectionIterator.nextByteAsInt();
                var renderSection = renderRegion.getSection(renderSectionId);

                if (renderSection == null) {
                    continue;
                }

                var context = renderSection.getBuiltContext();

                if (context instanceof MinecraftBuiltRenderSectionData<?,?> mcData) {
                    ((List<BLOCKENTITY>)mcData.culledBlockEntities).forEach(consumer);
                }
            }
        }

        for (var renderSection : globalSections) {
            var context = renderSection.getBuiltContext();

            if (context instanceof MinecraftBuiltRenderSectionData<?,?> mcData) {
                ((List<BLOCKENTITY>)mcData.globalBlockEntities).forEach(consumer);
            }
        }
    }
}
