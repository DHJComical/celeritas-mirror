package org.embeddedt.embeddium.impl.util;

import net.minecraft.util.profiling.ProfilerFiller;

public class ProfilerUtil {
    public static ProfilerFiller get() {
        //? if >=1.21.11 {
        /*return net.minecraft.util.profiling.Profiler.get();
        *///?} else
        return net.minecraft.client.Minecraft.getInstance().getProfiler();
    }
}
