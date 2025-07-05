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
}
