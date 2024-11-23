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
       return fromName(project.getName());
    }

    public static ModLoader fromName(String activeName) {
        if (activeName.endsWith("fabric")) {
            return FABRIC;
        }
        if (activeName.endsWith("neoforge")) {
            return NEOFORGE;
        }
        if (activeName.endsWith("forge")) {
            return FORGE;
        }
        return null;
    }

    public static String getMinecraftVersion(Project project) {
        return getMinecraftVersion(project.getName());
    }

    public static String getMinecraftVersion(String name) {
        int lastIdx = name.lastIndexOf('-');
        return lastIdx == -1 ? "unknown" : name.substring(0, lastIdx);
    }
}
