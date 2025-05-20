package net.irisshaders.iris.mixin;

import net.irisshaders.iris.shadows.frustum.MCAABB;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AABB.class)
public class MixinAABB implements MCAABB {
    @Final @Shadow public double minX;
    @Final @Shadow public double minY;
    @Final @Shadow public double minZ;
    @Final @Shadow public double maxX;
    @Final @Shadow public double maxY;
    @Final @Shadow public double maxZ;

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
