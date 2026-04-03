package org.embeddedt.embeddium.impl.util;

import net.minecraft.world.phys.Vec3;

public class VecUtil {
    public static Vec3 vec3FromRGB24(int color) {
        double r = (double)(color >> 16 & 0xff) / 255.0;
        double g = (double)(color >> 8 & 0xff) / 255.0;
        double b = (double)(color & 0xff) / 255.0;
        return new Vec3(r, g, b);
    }
}
