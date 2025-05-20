package org.taumc.celeritas.impl.render.terrain;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.taumc.celeritas.mixin.core.terrain.ActiveRenderInfoAccessor;

public class CameraHelper {
    public static Vector3f getThirdPersonOffset() {
        final Vector3f offset = new Vector3f(); // third person offset
        final Matrix4f inverseModelView = new Matrix4f(ActiveRenderInfoAccessor.getModelViewMatrix()).invert();
        inverseModelView.transformPosition(offset);
        return offset;
    }
}
