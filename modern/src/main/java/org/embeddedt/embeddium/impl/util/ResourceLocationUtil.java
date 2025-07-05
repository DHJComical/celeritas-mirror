package org.embeddedt.embeddium.impl.util;

import net.minecraft.resources.ResourceLocation;

public class ResourceLocationUtil {
    public static ResourceLocation make(String namespace, String path) {
        //? if >=1.21
        /*return ResourceLocation.fromNamespaceAndPath(namespace, path);*/
        //? if <1.21
        return new ResourceLocation(namespace, path);
    }

    public static ResourceLocation make(String str) {
        //? if >=1.21 {
        /*if(str.contains(":")) {
            return ResourceLocation.parse(str);
        } else {
            return ResourceLocation.withDefaultNamespace(str);
        }
        *///?} else
        return new ResourceLocation(str);
    }
}
