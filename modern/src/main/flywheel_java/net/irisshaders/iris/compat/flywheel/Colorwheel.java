package net.irisshaders.iris.compat.flywheel;

import net.minecraft.resources.ResourceLocation;
import org.embeddedt.embeddium.impl.loader.common.EarlyLoaderServices;
import org.embeddedt.embeddium.impl.util.ResourceLocationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared helpers for the Flywheel backend. Must not reference Flywheel classes, since Iris queries
 * {@link #isFlywheelLoaded()} whether or not Flywheel is installed.
 */
public final class Colorwheel
{
    public static final Logger LOGGER = LoggerFactory.getLogger("Celeritas/Flywheel");

    // The Colorwheel release whose shader pack contract (programs, properties, macros) this backend implements.
    public static final String FORMATTED_VERSION = "10300";

    private static final boolean FLYWHEEL_LOADED = EarlyLoaderServices.INSTANCE.isModLoaded("flywheel");

    private Colorwheel()
    {
    }

    public static boolean isFlywheelLoaded()
    {
        return FLYWHEEL_LOADED;
    }

    public static ResourceLocation rl(String path)
    {
        return ResourceLocationUtil.make("celeritas", path);
    }
}
