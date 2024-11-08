package org.embeddedt.embeddium.impl.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
//? if <1.18
/*import net.minecraft.world.level.block.EntityBlock;*/
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;

public class WorldUtil {
    public static int getMinBuildHeight(LevelReader level) {
        //? if >=1.21.2 {
        /*return level.getMinY();
        *///?} else if >=1.17 <1.21.2 {
        return level.getMinBuildHeight();
        //?} else
        /*return 0;*/
    }

    public static int getMaxBuildHeight(LevelReader level) {
        //? if >=1.21.2 {
        /*return level.getMaxY() + 1;
        *///?} else if >=1.17 <1.21.2 {
        return level.getMaxBuildHeight();
        //?} else
        /*return 255;*/
    }

    public static int getMinSection(LevelReader level) {
        //? if >=1.21.2 {
        /*return level.getMinSectionY();
        *///?} else if >=1.17 <1.21.2 {
        return level.getMinSection();
        //?} else
        /*return 0;*/
    }

    public static int getMaxSection(LevelReader level) {
        //? if >=1.21.2 {
        /*return level.getMaxSectionY() + 1;
        *///?} else if >=1.17 <1.21.2 {
        return level.getMaxSection();
        //?} else
        /*return 15;*/
    }

    public static int getSectionIndexFromSectionY(LevelReader level, int sectionY) {
        //? if >=1.17 {
        return level.getSectionIndexFromSectionY(sectionY);
        //?} else
        /*return sectionY;*/
    }

    public static boolean isSectionEmpty(LevelChunkSection section) {
        //? if >=1.17 {
        return section == null || section.hasOnlyAir();
        //?} else
        /*return LevelChunkSection.isEmpty(section);*/
    }

    public static boolean hasBlockEntity(BlockState state) {
        //? if >=1.18 {
        return state.hasBlockEntity();
        //?} else if forge {
        /*return state.hasTileEntity();
        *///?} else {
        /*return state instanceof EntityBlock;
        *///?}
    }

    public static int getLightEmission(BlockState state, BlockGetter world, BlockPos pos) {
        //? if forge && >=1.17 {
        return state.getLightEmission(world, pos);
        //?} else if forge {
        /*return state.getLightValue(world, pos);
        *///?} else {
        /*return state.getLightEmission();
        *///?}
    }

    public static int posToSectionCoord(double coord) {
        return posToSectionCoord(Mth.floor(coord));
    }

    public static int posToSectionCoord(int coord) {
        return coord >> 4;
    }

    public static int sectionToBlockCoord(int sec, int block) {
        return (sec << 4) + block;
    }
}
