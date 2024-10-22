package org.embeddedt.embeddium.impl.render.chunk;

import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import net.caffeinemc.mods.sodium.api.util.ColorMixer;
import org.embeddedt.embeddium.impl.render.ShaderModBridge;

public interface ChunkColorWriter {
    int writeColor(int colorWithAlpha, float aoValue);

    ChunkColorWriter LEGACY = ColorABGR::withAlpha;
    ChunkColorWriter EMBEDDIUM = (color, ao) -> ColorMixer.mulSingleWithoutAlpha(color, (int)(ao * 255));

    static ChunkColorWriter get() {
        return ShaderModBridge.emulateLegacyColorBrightnessFormat() ? LEGACY : EMBEDDIUM;
    }
}
