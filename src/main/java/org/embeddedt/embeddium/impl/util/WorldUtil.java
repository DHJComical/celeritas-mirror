package org.embeddedt.embeddium.impl.util;

import net.minecraft.world.level.LevelReader;

public class WorldUtil {
    public static int getMinBuildHeight(LevelReader level) {
        //? if >=1.21.2
        /*return level.getMinY();*/
        //? if <1.21.2
        return level.getMinBuildHeight();
    }

    public static int getMaxBuildHeight(LevelReader level) {
        //? if >=1.21.2
        /*return level.getMaxY() + 1;*/
        //? if <1.21.2
        return level.getMaxBuildHeight();
    }

    public static int getMinSection(LevelReader level) {
        //? if >=1.21.2
        /*return level.getMinSectionY();*/
        //? if <1.21.2
        return level.getMinSection();
    }

    public static int getMaxSection(LevelReader level) {
        //? if >=1.21.2
        /*return level.getMaxSectionY() + 1;*/
        //? if <1.21.2
        return level.getMaxSection();
    }
}
