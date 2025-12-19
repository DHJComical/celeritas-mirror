package org.taumc.celeritas.mixin.core;

import com.mojang.blaze3d.vertex.BufferBuilder;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BufferBuilder.class)
public interface TessellatorAccessor {
    //? if <1.7 {
    @org.spongepowered.asm.mixin.gen.Accessor("f_5537920")
    static void celeritas$setTriangleMode(boolean bl) {
        throw new AssertionError();
    }

    @org.spongepowered.asm.mixin.gen.Accessor("f_5537920")
    static boolean celeritas$getTriangleMode() {
        throw new AssertionError();
    }
    //?}
}
