package org.taumc.celeritas.impl.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.taumc.celeritas.impl.util.PlatformUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigMigrator {
    public static Logger LOGGER = LogManager.getLogger("Embeddium");

    /**
     * Tries to migrate the equivalent config file from Rubidium to the Embeddium name if possible.
     */
    public static Path handleConfigMigration(String fileName) {
        Path mainPath = PlatformUtil.getConfigDir().toPath().resolve(fileName);
        try {
            if(Files.notExists(mainPath)) {
                String legacyName = fileName.replace("assets/celeritas", "rubidium");
                Path legacyPath = PlatformUtil.getConfigDir().toPath().resolve(legacyName);
                if(Files.exists(legacyPath)) {
                    Files.move(legacyPath, mainPath);
                    LOGGER.warn("Migrated {} config file to {}", legacyName, fileName);
                }
            }
        } catch(IOException | RuntimeException e) {
            LOGGER.error("Exception encountered while attempting config migration", e);
        }

        return mainPath;
    }
}
