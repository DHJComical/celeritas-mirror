package org.embeddedt.embeddium.impl.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public class ClientUtil {
    public static boolean shouldEntityAppearGlowing(Entity entity) {
        //? if <1.17 {
        /*boolean glow = entity.isGlowing();
        *///?} else
        boolean glow = entity.isCurrentlyGlowing();

        if(glow) {
            return true;
        }

        var player = Minecraft.getInstance().player;
        return player != null && player.isSpectator() && Minecraft.getInstance().options.keySpectatorOutlines.isDown() && entity.getType() == EntityType.PLAYER;
    }
}
