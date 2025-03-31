package net.irisshaders.iris.gui.option;

import net.irisshaders.iris.pathways.colorspace.ColorSpace;
import org.taumc.celeritas.api.v0.CeleritasShadersApi;


public class IrisVideoSettings {
	public static int shadowDistance = 32;
	public static ColorSpace colorSpace = ColorSpace.SRGB;

	public static int getOverriddenShadowDistance(int base) {
		return CeleritasShadersApi.getInstance().getOverriddenShadowDistance(base);
	}

	public static boolean isShadowDistanceSliderEnabled() {
		return CeleritasShadersApi.getInstance().isShadowDistanceSliderEnabled();
	}
}
