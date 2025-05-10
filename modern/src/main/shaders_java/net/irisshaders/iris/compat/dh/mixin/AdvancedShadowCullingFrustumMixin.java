package net.irisshaders.iris.compat.dh.mixin;

import com.seibel.distanthorizons.api.interfaces.override.rendering.IDhApiShadowCullingFrustum;
import com.seibel.distanthorizons.api.objects.math.DhApiMat4f;
import net.irisshaders.iris.shadows.frustum.advanced.AdvancedShadowCullingFrustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = AdvancedShadowCullingFrustum.class, remap = false)
public class AdvancedShadowCullingFrustumMixin implements IDhApiShadowCullingFrustum {
    @Shadow
    private int worldMinYDH;
    @Shadow
    private int worldMaxYDH;
    @Shadow
    public int isVisible(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        throw new AssertionError();
    }

    @Override
    public void update(int worldMinBlockY, int worldMaxBlockY, DhApiMat4f worldViewProjection) {
        this.worldMinYDH = worldMinBlockY;
        this.worldMaxYDH = worldMaxBlockY;
    }

    @Override
    public boolean intersects(int lodBlockPosMinX, int lodBlockPosMinZ, int lodBlockWidth, int lodDetailLevel) {
        return this.isVisible(lodBlockPosMinX, this.worldMinYDH, lodBlockPosMinZ, lodBlockPosMinX + lodBlockWidth, this.worldMaxYDH, lodBlockPosMinZ + lodBlockWidth) != 0;
    }
}
