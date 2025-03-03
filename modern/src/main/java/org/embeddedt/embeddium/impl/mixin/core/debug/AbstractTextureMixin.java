package org.embeddedt.embeddium.impl.mixin.core.debug;

//? if <1.21.5-alpha.25.7.a {

import net.minecraft.client.renderer.texture.AbstractTexture;
import org.embeddedt.embeddium.impl.gl.debug.GLDebug;
import org.embeddedt.embeddium.impl.render.texture.NameableTexture;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractTexture.class)
public class AbstractTextureMixin implements NameableTexture {
    @Shadow
    protected int id;

    @Unique
    private String celeritas$name;

    @Unique
    private boolean celeritas$hasSetName;

    @Inject(method = "bind", at = @At("RETURN"))
    private void celeritas$applyName(CallbackInfo ci) {
        if (!this.celeritas$hasSetName && this.id != -1 && this.celeritas$name != null) {
            this.celeritas$hasSetName = true;
            GLDebug.nameObject(GL11.GL_TEXTURE, this.id, this.celeritas$name);
        }
    }

    @Override
    public void celeritas$setName(String name) {
        this.celeritas$name = name;
        this.celeritas$hasSetName = false;
    }
}

//?}