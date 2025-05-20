package org.taumc.celeritas.mixin.shaders;

import net.irisshaders.iris.shadows.frustum.MCAABB;
import net.minecraft.util.AxisAlignedBB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AxisAlignedBB.class)
public class MixinAxisAlignedBB_Shaders implements MCAABB  {
    @Shadow public double minX;
    @Shadow public double minY;
    @Shadow public double minZ;
    @Shadow public double maxX;
    @Shadow public double maxY;
    @Shadow public double maxZ;

    @Override
    public float minX() {
        return (float)minX;
    }
    @Override
    public float minY() {
        return (float)minY;
    }
    @Override
    public float minZ() {
        return (float)minZ;
    }
    @Override
    public float maxX() {
        return (float)maxX;
    }
    @Override
    public float maxY() {
        return (float)maxY;
    }
    @Override
    public float maxZ() {
        return (float)maxZ;
    }
}
