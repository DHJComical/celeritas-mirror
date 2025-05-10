package org.taumc.celeritas.mixin.core;

import net.minecraft.client.render.Tessellator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Tessellator.class)
public interface TessellatorAccessor {
    @Accessor("TRIANGLE_MODE")
    static void celeritas$setTriangleMode(boolean bl) {
        throw new AssertionError();
    }

    @Accessor("TRIANGLE_MODE")
    static boolean celeritas$getTriangleMode() {
        throw new AssertionError();
    }
}
