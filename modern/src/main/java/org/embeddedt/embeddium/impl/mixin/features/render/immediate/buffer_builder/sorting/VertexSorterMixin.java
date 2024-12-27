package org.embeddedt.embeddium.impl.mixin.features.render.immediate.buffer_builder.sorting;

//? if >=1.20 {
import com.mojang.blaze3d.vertex.VertexSorting;
import org.embeddedt.embeddium.impl.modern.sorting.VertexSorters;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(VertexSorting.class)
public interface VertexSorterMixin {
    /**
     * @author JellySquid
     * @reason Optimize vertex sorting
     */
    @Overwrite
    static VertexSorting byDistance(float x, float y, float z) {
        return VertexSorters.sortByDistance(new Vector3f(x, y, z));
    }
}
//?}