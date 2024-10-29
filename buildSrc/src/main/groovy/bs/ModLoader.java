package bs;

import org.gradle.api.Project;

public enum ModLoader {
    FABRIC("fabric", 1),
    FORGE("forge", -1),
    NEOFORGE("neoforge", -2);

    public final String friendlyName;
    public final int id;

    ModLoader(String loader, int id) {
        this.friendlyName = loader;
        this.id = id;
    }

    public static ModLoader fromProject(Project project) {
        if (project.getName().endsWith("fabric")) {
            return FABRIC;
        }
        if (project.getName().endsWith("neoforge")) {
            return NEOFORGE;
        }
        if (project.getName().endsWith("forge")) {
            return FORGE;
        }
        return null;
    }
}
