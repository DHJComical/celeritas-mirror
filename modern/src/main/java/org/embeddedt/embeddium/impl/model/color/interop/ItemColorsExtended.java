package org.embeddedt.embeddium.impl.model.color.interop;

//? if <1.21.4-alpha.24.45.a {
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.world.item.ItemStack;

public interface ItemColorsExtended {
    ItemColor sodium$getColorProvider(ItemStack stack);
}
//?}