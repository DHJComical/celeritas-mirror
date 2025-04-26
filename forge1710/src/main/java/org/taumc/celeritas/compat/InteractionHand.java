package org.taumc.celeritas.compat;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public enum InteractionHand {
    MAIN_HAND,
    OFF_HAND;

    InteractionHand() {
    }

    public ItemStack getItemInHand(EntityPlayer player){
        return player.getHeldItem();
    }
}
