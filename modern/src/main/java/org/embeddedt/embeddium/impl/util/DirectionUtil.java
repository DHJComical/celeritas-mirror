package org.embeddedt.embeddium.impl.util;

import java.util.Arrays;

import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import org.embeddedt.embeddium.api.util.NormI8;

/**
 * Contains a number of cached arrays to avoid allocations since calling Enum#values() requires the backing array to
 * be cloned every time.
 */
public class DirectionUtil {
    public static final Direction[] ALL_DIRECTIONS = Direction.values();

    // Provides the same order as enumerating Direction and checking the axis of each value
    public static final Direction[] HORIZONTAL_DIRECTIONS = new Direction[] { Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST };

    private static final Direction[] OPPOSITE_DIRECTIONS = Arrays.stream(ALL_DIRECTIONS)
            .map(Direction::getOpposite)
            .toArray(Direction[]::new);

    public static final int[] PACKED_NORMALS = new int[Direction.values().length];

    static {
        Direction[] directions = Direction.values();
        for (int i = 0; i < directions.length; i++) {
            //? if <1.21.5-alpha.25.6.a {
            Vec3i normal = directions[i].getNormal();
            //?} else
            /*Vec3i normal = directions[i].getUnitVec3i();*/
            PACKED_NORMALS[i] = NormI8.pack(normal.getX(), normal.getY(), normal.getZ());
        }
    }

    // Direction#byId is slow in the absence of Lithium
    public static Direction getOpposite(Direction dir) {
        return OPPOSITE_DIRECTIONS[dir.ordinal()];
    }
}
