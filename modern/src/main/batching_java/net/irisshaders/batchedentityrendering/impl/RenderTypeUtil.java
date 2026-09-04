package net.irisshaders.batchedentityrendering.impl;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;

public class RenderTypeUtil {
	public static boolean isTriangleStripDrawMode(RenderType renderType) {
		return renderType.mode() == VertexFormat.Mode.TRIANGLE_STRIP;
	}

    public static boolean requiresSegmentSplits(RenderType renderType) {
        var mode = renderType.mode();
        return mode == VertexFormat.Mode.TRIANGLE_FAN || mode == VertexFormat.Mode.DEBUG_LINE_STRIP || mode == VertexFormat.Mode.LINE_STRIP;
    }

    /**
     * Determines which drawing phase a render type belongs to, unwrapping any Iris render type wrappers first.
     */
    public static TransparencyType getTransparencyType(RenderType type) {
        while (type instanceof WrappableRenderType) {
            type = ((WrappableRenderType) type).unwrap();
        }

        if (type instanceof BlendingStateHolder) {
            return ((BlendingStateHolder) type).getTransparencyType();
        }

        // Default to "generally transparent" if we can't figure it out. This is the conservative choice, since it means
        // the geometry is drawn in submission order rather than being reordered.
        return TransparencyType.GENERAL_TRANSPARENT;
    }
}
