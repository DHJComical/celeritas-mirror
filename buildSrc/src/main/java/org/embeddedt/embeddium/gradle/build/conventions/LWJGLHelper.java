package org.embeddedt.embeddium.gradle.build.conventions;

import org.gradle.api.Project;

import java.util.List;
import java.util.Objects;

public class LWJGLHelper {
    private static final String LWJGL3_VERSION = "3.3.3";
    private static final List<String> LWJGL3_COMPONENTS = List.of(
            "lwjgl",
            "lwjgl-opengl",
            "lwjgl-openal",
            "lwjgl-glfw",
            "lwjgl-stb"
    );

    public static void convertLwjgl2To3(Project project) {
        convertLwjgl2To3(project, "minecraftLibraries");
    }

    public static void convertLwjgl2To3(Project project, String minecraftLibsConfigurationName) {
        project.getConfigurations().getByName(minecraftLibsConfigurationName).getDependencies().removeIf(dep -> Objects.equals(dep.getGroup(), "org.lwjgl.lwjgl"));
        String legacyLwjgl3Version = Objects.requireNonNull(project.getProperties().get("legacy_lwjgl3_version"), "legacy_lwjgl3_version not specified").toString();
        project.getDependencies().add(minecraftLibsConfigurationName, "org.taumc:legacy-lwjgl3:" + legacyLwjgl3Version);
        addLwjgl3(project, minecraftLibsConfigurationName);
    }

    public static void addLwjgl3(Project project) {
        addLwjgl3(project, "implementation");
    }

    public static void addLwjgl3(Project project, String configurationName) {
        var deps = project.getDependencies();
        for (String component : LWJGL3_COMPONENTS) {
            deps.add(configurationName, "org.lwjgl:" + component + ":" + LWJGL3_VERSION);
            deps.add(configurationName, "org.lwjgl:" + component + ":" + LWJGL3_VERSION + ":natives-" + "linux");
            deps.add(configurationName, "org.lwjgl:" + component + ":" + LWJGL3_VERSION + ":natives-" + "windows");
            deps.add(configurationName, "org.lwjgl:" + component + ":" + LWJGL3_VERSION + ":natives-" + "macos-arm64");
        }
    }
}
