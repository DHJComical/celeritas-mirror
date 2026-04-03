package org.embeddedt.embeddium.impl.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
//? if <1.16
/*import net.minecraft.world.entity.EntityType;*/

public class ClientUtil {
    public static boolean shouldEntityAppearGlowing(Entity entity) {
        //? if <1.16 {
        /*boolean glow = entity.isGlowing();

        if(glow) {
            return true;
        }

        var player = Minecraft.getInstance().player;
        return player != null && player.isSpectator() && Minecraft.getInstance().options.keySpectatorOutlines.isDown() && entity.getType() == EntityType.PLAYER;
        *///?} else
        return Minecraft.getInstance().shouldEntityAppearGlowing(entity);
    }
}
