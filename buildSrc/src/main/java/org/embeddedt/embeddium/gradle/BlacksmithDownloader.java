package org.embeddedt.embeddium.gradle;

import org.gradle.api.Project;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class BlacksmithDownloader {
    private static final String BLACKSMITH_VERSION = "0.1.0-34.g9c8ecb5";
    public static File getBlacksmithJar(Project project) {
        // Make sure the build directory exists
        project.getLayout().getBuildDirectory().get().getAsFile().mkdirs();

        File blacksmithJar = project.getLayout().getBuildDirectory().file("blacksmith-" + BLACKSMITH_VERSION + ".jar").get().getAsFile();
        if(!blacksmithJar.exists()) {
            // Need to download it
            try {
                URL url = new URL("https://maven.taumc.org/releases/org/taumc/blacksmith/" + BLACKSMITH_VERSION + "/blacksmith-" + BLACKSMITH_VERSION + ".jar");
                try(InputStream is = url.openStream()) {
                    byte[] blacksmith = is.readAllBytes();
                    try(FileOutputStream fos = new FileOutputStream(blacksmithJar)) {
                        fos.write(blacksmith);
                    }
                }
            } catch(IOException e) {
                blacksmithJar.delete();
                throw new RuntimeException(e);
            }
        }
        return blacksmithJar;
    }
}
