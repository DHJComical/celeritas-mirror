package net.irisshaders.iris.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import org.embeddedt.embeddium.compat.mc.MCNativeImage;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(NativeImage.class)
public abstract class MixinNativeImage implements MCNativeImage {
}
