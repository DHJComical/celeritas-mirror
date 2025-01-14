package org.embeddedt.embeddium.impl.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
//? if <1.18
/*import net.minecraft.world.level.block.EntityBlock;*/
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
//? if forgelike && <1.16
/*import net.minecraftforge.client.model.pipeline.LightUtil;*/

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
        //? if >=1.18 {
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
        /*return state.getBlock() instanceof EntityBlock;
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

    public static boolean isDebug(Level level) {
        //? if >=1.16 {
        return level.isDebug();
        //?} else
        /*return level.getGeneratorType() == net.minecraft.world.level.LevelType.DEBUG_ALL_BLOCK_STATES;*/
    }

    public static boolean hasSkyLight(Level level) {
        //? if >=1.16 {
        return level.dimensionType().hasSkyLight();
        //?} else
        /*return level.getDimension().isHasSkyLight();*/
    }

    public static float getShade(BlockAndTintGetter getter, Direction lightFace, boolean shade) {
        //? if >=1.16 {
        return getter.getShade(lightFace, shade);
        //?} else if forgelike && <1.16 {
        /*return shade ? LightUtil.diffuseLight(lightFace) : 1.0f;
        *///?} else
        /*return 1.0f;*/
    }
}
