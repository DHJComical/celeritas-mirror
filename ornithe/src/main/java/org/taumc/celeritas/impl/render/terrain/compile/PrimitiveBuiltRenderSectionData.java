package org.taumc.celeritas.impl.render.terrain.compile;

import net.minecraft.block.entity.BlockEntity;
import org.embeddedt.embeddium.impl.render.chunk.data.MinecraftBuiltRenderSectionData;

import java.util.Objects;

public class PrimitiveBuiltRenderSectionData extends MinecraftBuiltRenderSectionData<Object, BlockEntity> {
    public boolean hasSkyLight;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        PrimitiveBuiltRenderSectionData that = (PrimitiveBuiltRenderSectionData) o;
        return hasSkyLight == that.hasSkyLight;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), hasSkyLight);
    }
}
