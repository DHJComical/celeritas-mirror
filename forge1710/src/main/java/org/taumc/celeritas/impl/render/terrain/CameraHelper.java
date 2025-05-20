package org.taumc.celeritas.impl.render.terrain;

import net.minecraft.client.Minecraft;
import org.joml.Vector3d;

public class CameraHelper {
    public static Vector3d getCurrentCameraPosition(double partialTicks) {
        var entityIn = Minecraft.getMinecraft().renderViewEntity;
        return new Vector3d(
            entityIn.prevPosX + (entityIn.posX - entityIn.prevPosX) * partialTicks,
            entityIn.prevPosY + (entityIn.posY - entityIn.prevPosY) * partialTicks,
            entityIn.prevPosZ + (entityIn.posZ - entityIn.prevPosZ) * partialTicks
        );
    }
}
