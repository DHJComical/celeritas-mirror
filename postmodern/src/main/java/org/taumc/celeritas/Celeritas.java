package org.taumc.celeritas;

import net.neoforged.fml.common.Mod;
import org.embeddedt.embeddium.impl.gl.device.GLRenderDevice;

@Mod("celeritas")
public class Celeritas {
    public Celeritas() {
        GLRenderDevice.VANILLA_STATE_RESETTER = () -> {

        };
    }
}
