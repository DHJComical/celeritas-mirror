package org.embeddedt.embeddium.impl.render.chunk.lists;

import org.embeddedt.embeddium.impl.render.chunk.ChunkUpdateType;
import org.embeddedt.embeddium.impl.render.chunk.RenderSection;

import java.util.ArrayDeque;
import java.util.EnumMap;
import java.util.Map;

/**
 * The list of render sections that need to be rebuilt.
 * @param byUpdateType a map from update type to the appropriate list of sections
 * @param hasAdditionalUpdates whether there were additional updates not queued for efficiency reasons
 */
public record ChunkRebuildLists(Map<ChunkUpdateType, ArrayDeque<RenderSection>> byUpdateType, boolean hasAdditionalUpdates) {
    public static final ChunkRebuildLists EMPTY;

    static {
        Map<ChunkUpdateType, ArrayDeque<RenderSection>> rebuildLists = new EnumMap<>(ChunkUpdateType.class);

        for (var type : ChunkUpdateType.values()) {
            rebuildLists.put(type, new ArrayDeque<>());
        }

        EMPTY = new ChunkRebuildLists(rebuildLists, false);
    }
}
