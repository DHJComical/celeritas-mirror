package org.embeddedt.embeddium.impl.mixin.core.debug;

//? if <1.21.5-alpha.25.7.a {

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.embeddedt.embeddium.impl.gl.debug.GLDebug;
import org.embeddedt.embeddium.impl.render.texture.NameableTexture;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractTexture.class)
public class AbstractTextureMixin implements NameableTexture {
    @Shadow
    protected int id;
    @Unique
    private String celeritas$name;

    @ModifyExpressionValue(method = "getId", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/TextureUtil;generateTextureId()I"))
    private int assignName(int id) {
        if (id != -1 && celeritas$name != null) {
            GLDebug.nameObject(GL11.GL_TEXTURE, id, celeritas$name);
        }
        return id;
    }

    @Override
    public void celeritas$setName(String name) {
        this.celeritas$name = name;
        if (RenderSystem.isOnRenderThread() && this.id != -1) {
            GLDebug.nameObject(GL11.GL_TEXTURE, this.id, name);
        }
    }
}

//?}