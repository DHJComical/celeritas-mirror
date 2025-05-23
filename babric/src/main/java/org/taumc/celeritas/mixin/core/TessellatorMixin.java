package org.taumc.celeritas.mixin.core;

import com.mojang.blaze3d.vertex.BufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.taumc.celeritas.impl.extensions.TessellatorExtension;

@Mixin(BufferBuilder.class)
public abstract class TessellatorMixin implements TessellatorExtension {
    @Shadow
    protected abstract void clear();

    @Shadow
    private int[] buffer;

    @Shadow
    private int vertexCount;

    @Shadow
    private boolean tessellating;

    @Override
    public int[] celeritas$getRawBuffer() {
        return buffer;
    }

    @Override
    public int celeritas$getVertexCount() {
        return vertexCount;
    }

    @Override
    public void celeritas$reset() {
        this.tessellating = false;
        this.clear();
    }
}

