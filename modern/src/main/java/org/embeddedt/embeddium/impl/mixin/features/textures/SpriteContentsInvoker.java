package org.embeddedt.embeddium.impl.mixin.features.textures;

//? if >=1.20 {
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(SpriteContents.class)
public interface SpriteContentsInvoker {
    @Invoker
    void invokeUpload(int x, int y, int unpackSkipPixels, int unpackSkipRows, NativeImage[] images
        /*? if >=1.21.5-alpha.25.7.a {*//*, com.mojang.blaze3d.textures.GpuTexture tex*//*?}*/
    );
}
//?}