package org.embeddedt.embeddium.impl.util;

import net.minecraft.client.Minecraft;
//? if >=1.21.2
/*import net.minecraft.util.profiling.Profiler;*/
import net.minecraft.util.profiling.ProfilerFiller;

public class ProfilerUtil {
    public static ProfilerFiller get() {
        //? if <1.21.2
        return Minecraft.getInstance().getProfiler();
        //? if >=1.21.2
        /*return Profiler.get();*/

    }
}
