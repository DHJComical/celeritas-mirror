package org.embeddedt.embeddium.gradle.stonecutter.versionmanagement;

import com.google.gson.Gson;
import org.gradle.api.initialization.Settings;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.function.Predicate;

public class VersionLocalJson {
    private static final Gson GSON = new Gson();
    public List<String> includedVersions;

    public VersionLocalJson() {
        this.includedVersions = List.of();
    }

    public Predicate<String> getVersionFilterPredicate() {
        if(this.includedVersions.isEmpty()) {
            return v -> true;
        } else {
            return this.includedVersions::contains;
        }
    }

    public static VersionLocalJson read(Settings projectSettings) throws IOException {
        return GSON.fromJson(Files.readString(projectSettings.getRootDir().toPath().resolve("versions-local.json")), VersionLocalJson.class);
    }
}
