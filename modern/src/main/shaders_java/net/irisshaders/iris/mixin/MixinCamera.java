package net.irisshaders.iris.mixin;

import net.minecraft.client.Camera;
import org.embeddedt.embeddium.compat.mc.MCCamera;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Camera.class)
public class MixinCamera implements MCCamera {
}
