package org.taumc.celeritas.impl.util;


import net.minecraft.util.ResourceLocation;

public class ResourceLocationUtil {
    public static ResourceLocation make(String namespace, String path) {
        return new ResourceLocation(namespace, path);
    }

    public static ResourceLocation make(String str) {
        return new ResourceLocation(str);
    }
}
