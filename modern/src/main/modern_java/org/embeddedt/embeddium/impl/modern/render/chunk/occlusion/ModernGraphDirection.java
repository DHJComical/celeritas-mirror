package org.embeddedt.embeddium.impl.modern.render.chunk.occlusion;

import net.minecraft.core.Direction;

import static org.embeddedt.embeddium.impl.render.chunk.occlusion.GraphDirection.*;

public class ModernGraphDirection {
    private static final Direction[] ENUMS;

    static {
        ENUMS = new Direction[COUNT];
        ENUMS[DOWN] = Direction.DOWN;
        ENUMS[UP] = Direction.UP;
        ENUMS[NORTH] = Direction.NORTH;
        ENUMS[SOUTH] = Direction.SOUTH;
        ENUMS[WEST] = Direction.WEST;
        ENUMS[EAST] = Direction.EAST;
    }
    public static Direction toEnum(int direction) {
        return ENUMS[direction];
    }
}
