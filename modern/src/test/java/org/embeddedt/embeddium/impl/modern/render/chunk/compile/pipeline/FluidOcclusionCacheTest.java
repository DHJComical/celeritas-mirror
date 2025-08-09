package org.embeddedt.embeddium.impl.modern.render.chunk.compile.pipeline;

import net.minecraft.world.phys.shapes.Shapes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FluidOcclusionCacheTest {
    private final FluidOcclusionCache cache = new FluidOcclusionCache();

    @Test
    void testFullBlockHidesFluidFace() {
        assertFalse(cache.isTopFluidFacePotentiallyVisible(Shapes.block()));
    }

    @Test
    void testSlabHidesFluidFace() {
        assertFalse(cache.isTopFluidFacePotentiallyVisible(FluidOcclusionCache.UPPER_HALF_SLAB));
    }

    @Test
    void testThinSlabDoesNotHideFluidFace() {
        assertTrue(cache.isTopFluidFacePotentiallyVisible(Shapes.box(0.0, 0.7, 0.0, 1.0, 1.0, 1.0)));
    }

    @Test
    void testInsetBlockDoesNotHideFluidFace() {
        double inset = 0.1;
        assertTrue(cache.isTopFluidFacePotentiallyVisible(Shapes.box(inset, 0.5, inset, 1.0 - inset, 1.0, 1.0 - inset)));
    }
}