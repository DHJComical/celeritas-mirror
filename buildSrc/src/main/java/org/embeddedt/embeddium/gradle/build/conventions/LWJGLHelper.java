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
        project.getConfigurations().getByName("minecraftLibraries").getDependencies().removeIf(dep -> Objects.equals(dep.getGroup(), "org.lwjgl.lwjgl"));
        project.getDependencies().add("minecraftLibraries", "org.taumc:legacy-lwjgl3:1.2-tau");
        addLwjgl3(project, "minecraftLibraries");
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
        }
    }
}
