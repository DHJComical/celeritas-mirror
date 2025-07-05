package org.embeddedt.embeddium.impl.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.embeddedt.embeddium.compat.mc.PlatformUtilService.PLATFORM_UTIL;

public class ConfigMigrator {
    public static Logger LOGGER = LogManager.getLogger("Embeddium");

    /**
     * Tries to migrate the equivalent config file from Rubidium to the Embeddium name if possible.
     */
    public static Path handleConfigMigration(String fileName) {
        Path mainPath = PLATFORM_UTIL.getConfigDir().resolve(fileName);
        try {
            if(Files.notExists(mainPath)) {
                String legacyName = fileName.replace("embeddium", "rubidium");
                Path legacyPath = PLATFORM_UTIL.getConfigDir().resolve(legacyName);
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
