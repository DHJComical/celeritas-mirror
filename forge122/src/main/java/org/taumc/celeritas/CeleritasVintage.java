package org.taumc.celeritas;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.embeddedt.embeddium.impl.gl.device.GLRenderDevice;

@Mod(modid = CeleritasVintage.MODID, useMetadata = true)
public class CeleritasVintage {
    public static final String MODID = "celeritas";
    private static final Logger LOGGER = LogManager.getLogger("Celeritas");

    @Mod.EventHandler
    public void onInit(FMLInitializationEvent event) {
        LOGGER.info("Hello from Forge!");
        GLRenderDevice.VANILLA_STATE_RESETTER = () -> {
            OpenGlHelper.glBindBuffer(OpenGlHelper.GL_ARRAY_BUFFER, 0);
        };
    }

    public static Logger logger() {
        return LOGGER;
    }
}
