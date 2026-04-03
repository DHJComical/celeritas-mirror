package org.embeddedt.embeddium.impl.util;

import net.minecraft.client.Minecraft;
import net.minecraft.util.profiling.ProfilerFiller;

public class ProfilerUtil {
    public static ProfilerFiller get() {
        return Minecraft.getInstance().getProfiler();
    }
}
