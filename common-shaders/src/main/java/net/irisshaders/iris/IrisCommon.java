package net.irisshaders.iris;

import java.nio.file.Path;

import static org.embeddedt.embeddium.compat.mc.PlatformUtilService.PLATFORM_UTIL;

public class IrisCommon {
    public static final float DEGREES_TO_RADIANS = (float)Math.PI / 180.0F;
    private static Path shaderpacksDirectory;

    public static Path getShaderpacksDirectory() {
        if (shaderpacksDirectory == null) {
            shaderpacksDirectory = PLATFORM_UTIL.getGameDir().resolve("shaderpacks");
        }

        return shaderpacksDirectory;
    }
}
