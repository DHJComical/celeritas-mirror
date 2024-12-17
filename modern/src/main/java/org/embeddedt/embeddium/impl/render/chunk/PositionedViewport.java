package org.embeddedt.embeddium.impl.render.chunk;

import org.embeddedt.embeddium.impl.render.viewport.Viewport;
import org.joml.RoundingMode;
import org.joml.Vector3d;
import org.joml.Vector3i;

public record PositionedViewport(Viewport viewport, Vector3d position) {
    public Vector3i blockPosition() {
        Vector3i vec3i = new Vector3i();
        position.get(RoundingMode.FLOOR, vec3i);
        return vec3i;
    }
}
