package org.taumc.celeritas;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = CeleritasVintage.MODID, useMetadata = true)
public class CeleritasVintage {
    public static final String MODID = "celeritas";
    private static final Logger LOGGER = LogManager.getLogger("Celeritas");

    @Mod.EventHandler
    public void onInit(FMLInitializationEvent event) {
        LOGGER.info("Hello from Forge!");
    }

    public static Logger logger() {
        return LOGGER;
    }
}
