package org.embeddedt.embeddium.impl.modern.services;

import org.embeddedt.embeddium.impl.common.datastructure.ContextBundle;
import org.embeddedt.embeddium.impl.modern.render.chunk.ModernRenderSectionBuiltInfo;
import org.embeddedt.embeddium.impl.render.chunk.RenderSection;
import org.embeddedt.embeddium.impl.render.chunk.lists.RenderVisualsService;

public class ModernRenderVisualsService implements RenderVisualsService {
    private static final int MODERN_HAS_GEO = ModernRenderSectionBuiltInfo.HAS_BLOCK_GEOMETRY.id();
    private static final int MODERN_HAS_SPRITE = ModernRenderSectionBuiltInfo.ANIMATED_SPRITES.id();
    private static final int MODERN_HAS_CULLED_BE = ModernRenderSectionBuiltInfo.CULLED_BLOCK_ENTITIES.id();
    private static final int MODERN_HAS_GLOBAL_BE = ModernRenderSectionBuiltInfo.GLOBAL_BLOCK_ENTITIES.id();

    @Override
    public int getVisualBitmaskForSection(ContextBundle<RenderSection> renderData) {
        long modernFlags = renderData.getPopulatedIds();
        int genericFlags = ((int)(modernFlags >>> MODERN_HAS_GEO) & 1) << RenderVisualsService.HAS_BLOCK_GEOMETRY;
        genericFlags |= ((int)(modernFlags >>> MODERN_HAS_SPRITE) & 1) << RenderVisualsService.HAS_SPRITES;
        genericFlags |= ((int)(modernFlags >>> MODERN_HAS_CULLED_BE) & 1) << RenderVisualsService.HAS_BLOCK_ENTITIES;
        genericFlags |= ((int)(modernFlags >>> MODERN_HAS_GLOBAL_BE) & 1) << RenderVisualsService.HAS_BLOCK_ENTITIES;
        return genericFlags;
    }
}
