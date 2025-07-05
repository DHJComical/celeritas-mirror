package org.embeddedt.embeddium.impl.render.chunk.data;

import org.embeddedt.embeddium.impl.render.chunk.lists.RenderVisualsService;
import org.jetbrains.annotations.MustBeInvokedByOverriders;

import java.util.Objects;

/**
 * Class that holds context about the built data for a given render section. This class can be extended by implementations
 * to hold additional context and provide additional functionality.
 */
public class BuiltRenderSectionData {
    public boolean hasBlockGeometry;
    public long visibilityData;

    public int getVisualBitmaskForSection() {
        return this.hasBlockGeometry ? (1 << RenderVisualsService.HAS_BLOCK_GEOMETRY) : 0;
    }

    @MustBeInvokedByOverriders
    public void bake() {

    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        BuiltRenderSectionData that = (BuiltRenderSectionData) o;
        return hasBlockGeometry == that.hasBlockGeometry && visibilityData == that.visibilityData;
    }

    @Override
    public int hashCode() {
        return Objects.hash(hasBlockGeometry, visibilityData);
    }
}
