package org.embeddedt.embeddium.gradle.build.conventions;

import org.gradle.api.Project;

import java.util.List;

public class LWJGLHelper {
    private static final String LWJGL3_VERSION = "3.3.3";
    private static final List<String> LWJGL3_COMPONENTS = List.of(
            "lwjgl",
            "lwjgl-opengl",
            "lwjgl-glfw",
            "lwjgl-stb"
    );

    public static void addLwjgl3(Project project) {
        var deps = project.getDependencies();
        for (String component : LWJGL3_COMPONENTS) {
            deps.add("implementation", "org.lwjgl:" + component + ":" + LWJGL3_VERSION);
            deps.add("implementation", "org.lwjgl:" + component + ":" + LWJGL3_VERSION + ":natives-" + "linux");
        }
    }
}
