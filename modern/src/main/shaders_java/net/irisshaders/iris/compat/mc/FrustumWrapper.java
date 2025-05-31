package net.irisshaders.iris.compat.mc;

import net.irisshaders.iris.shadows.frustum.CommonFrustum;
import net.irisshaders.iris.shadows.frustum.MCAABB;
import net.irisshaders.iris.shadows.frustum.MCFrustum;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import org.embeddedt.embeddium.impl.render.viewport.Viewport;
import org.embeddedt.embeddium.impl.render.viewport.ViewportProvider;
import org.joml.Matrix4f;

public class FrustumWrapper extends Frustum implements MCFrustum, ViewportProvider {
    private static final Matrix4f emptyProjection = new Matrix4f();
    private static final Matrix4f emptyFrustum = new Matrix4f();

    protected CommonFrustum frustum;

    public FrustumWrapper(CommonFrustum mcFrustum) {
        super(emptyProjection.identity(), emptyFrustum.identity());
        this.frustum = mcFrustum;
    }

    @Override
    public void prepare(double camX, double camY, double camZ) {
        super.prepare(camX, camY, camZ);
        this.frustum.prepare(camX, camY, camZ);
    }

    @Override
    public boolean isVisible(MCAABB aabb) {
        return this.frustum.isVisible(aabb);
    }

    @Override
    public boolean isVisible(AABB aabb) {
        return isVisible((MCAABB) aabb);
    }

    @Override
    public Viewport sodium$createViewport() {
        return this.frustum.sodium$createViewport();
    }
}
