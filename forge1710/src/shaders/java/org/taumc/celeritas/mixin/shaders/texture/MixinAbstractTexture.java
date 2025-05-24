package org.taumc.celeritas.mixin.shaders.texture;

import net.irisshaders.iris.texture.TextureTracker;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.embeddedt.embeddium.compat.mc.MCAbstractTexture;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.mitchej123.glsm.GLStateManagerService.GL_STATE_MANAGER;

@Mixin(AbstractTexture.class)
public class MixinAbstractTexture implements MCAbstractTexture {
    @Shadow
    public int glTextureId;

    @Shadow
    public int getGlTextureId() {
        throw new IllegalStateException("Mixin shadow method should not be called");
    }

    @Shadow
    public void deleteGlTexture() {
        throw new IllegalStateException("Mixin shadow method should not be called");
    }

    // Inject after the newly-generated texture ID has been stored into the id field
    @Inject(method = "getGlTextureId()I", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/client/renderer/texture/AbstractTexture;glTextureId:I", shift = At.Shift.AFTER))
    private void iris$afterGenerateId(CallbackInfoReturnable<Integer> cir) {
        TextureTracker.INSTANCE.trackTexture(glTextureId, this);
    }

    // MCAbstractTexture implementation
    @Override
    public int getId() {
        return getGlTextureId();
    }

    @Override
    public void releaseId() {
        deleteGlTexture();
    }

    @Override
    public void bind() {
        GL_STATE_MANAGER.bindTexture(getGlTextureId());
    }

    @Override
    public void close() {
        // NOOP for AbstractTexture - subclasses might implement it
    }

}
