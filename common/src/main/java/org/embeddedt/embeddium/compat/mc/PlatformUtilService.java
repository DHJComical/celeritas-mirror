package org.embeddedt.embeddium.compat.mc;

import java.nio.file.Path;
import java.util.ServiceLoader;

/**
 * Service for providing platform-specific utilities.
 */
public interface PlatformUtilService {
    PlatformUtilService PLATFORM_UTIL = ServiceLoader.load(PlatformUtilService.class).findFirst().orElseThrow();

    boolean isLoadValid();

    boolean modPresent(String modid);

    String getModName(String modId);

    boolean isDevelopmentEnvironment();

    Path getConfigDir();

    Path getGameDir();
}
