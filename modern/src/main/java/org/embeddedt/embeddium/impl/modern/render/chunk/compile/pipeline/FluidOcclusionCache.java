package org.embeddedt.embeddium.impl.modern.render.chunk.compile.pipeline;

import it.unimi.dsi.fastutil.objects.Object2BooleanLinkedOpenHashMap;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FluidOcclusionCache {
    private final Object2BooleanLinkedOpenHashMap<VoxelShape> map = new Object2BooleanLinkedOpenHashMap<>(512, 0.5F);
    static final VoxelShape UPPER_HALF_SLAB = Shapes.box(0.0, 0.5, 0.0, 1.0, 1.0, 1.0);

    public boolean isTopFluidFacePotentiallyVisible(VoxelShape shape) {
        if (shape == Shapes.block()) {
            return false;
        } else {
            boolean result = map.computeIfAbsent(shape, FluidOcclusionCache::doesShapeIntersectSlab);
            if (map.size() > 512) {
                map.removeFirstBoolean();
            }
            return result;
        }
    }

    private static boolean doesShapeIntersectSlab(VoxelShape shape) {
        return Shapes.joinIsNotEmpty(UPPER_HALF_SLAB, shape, BooleanOp.ONLY_FIRST);
    }
}
