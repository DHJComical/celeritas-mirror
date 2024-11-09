package org.embeddedt.embeddium.gradle.stonecutter.versionmanagement;

import com.google.gson.Gson;
import org.gradle.api.initialization.Settings;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class VersionJson {
    private static final Gson GSON = new Gson();
    public String vcsVersion;
    public List<Version> versions;

    public static VersionJson read(Settings projectSettings) throws IOException {
        return GSON.fromJson(Files.readString(projectSettings.getRootDir().toPath().resolve("versions.json")), VersionJson.class);
    }
}
