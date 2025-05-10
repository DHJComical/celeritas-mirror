package org.taumc.celeritas.mixin.core;

import net.minecraft.client.render.Tessellator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.taumc.celeritas.impl.extensions.TessellatorExtension;

@Mixin(Tessellator.class)
public abstract class TessellatorMixin implements TessellatorExtension {
    @Shadow
    protected abstract void reset();

    @Shadow
    private int[] buffer;

    @Shadow
    private int vertexCount;

    @Shadow
    private boolean drawing;

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
        this.drawing = false;
        this.reset();
    }
}

