package net.irisshaders.iris.gui.option;

import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pathways.colorspace.ColorSpace;
import net.irisshaders.iris.pathways.scaling.RenderScalePreset;
import net.irisshaders.iris.pathways.scaling.UpscaleMode;

public class IrisVideoSettings {
	public static int shadowDistance = 32;
	public static ColorSpace colorSpace = ColorSpace.SRGB;
	public static RenderScalePreset renderScale = RenderScalePreset.NATIVE;
	public static UpscaleMode upscaleMode = UpscaleMode.FSR;
	public static int upscaleSharpness = 80;

	public static int getOverriddenShadowDistance(int base) {
		return Iris.getPipelineManager().getPipeline()
			.map(pipeline -> pipeline.getForcedShadowRenderDistanceChunksForDisplay().orElse(base))
			.orElse(base);
	}

	public static boolean isShadowDistanceSliderEnabled() {
		return Iris.getPipelineManager().getPipeline()
			.map(pipeline -> !pipeline.getForcedShadowRenderDistanceChunksForDisplay().isPresent())
			.orElse(true);
	}
}
