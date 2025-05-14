package org.taumc.celeritas.mixin.shaders;

import net.irisshaders.iris.api.v0.item.IrisItemLightProvider;
import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Item.class)
public class MixinItem_Shaders implements IrisItemLightProvider {
    // Interface Injection
}
