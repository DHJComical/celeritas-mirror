package org.embeddedt.embeddium.impl.modern.util;

import net.minecraft.core.Direction;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.jetbrains.annotations.Nullable;

import static org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing.*;

public class ModernQuadFacing {
    public static ModelQuadFacing fromDirection(Direction dir) {
        return switch (dir) {
            case DOWN   -> NEG_Y;
            case UP     -> POS_Y;
            case NORTH  -> NEG_Z;
            case SOUTH  -> POS_Z;
            case WEST   -> NEG_X;
            case EAST   -> POS_X;
        };
    }

    public static Direction toDirection(ModelQuadFacing facing) {
        return switch (facing) {
            case NEG_Y -> Direction.DOWN;
            case POS_Y -> Direction.UP;
            case NEG_Z -> Direction.NORTH;
            case POS_Z -> Direction.SOUTH;
            case NEG_X -> Direction.WEST;
            case POS_X -> Direction.EAST;
            case UNASSIGNED -> throw new IllegalArgumentException();
        };
    }

    public static ModelQuadFacing fromDirectionOrUnassigned(@Nullable Direction dir) {
        if (dir == null) {
            return UNASSIGNED;
        }
        return fromDirection(dir);
    }
}
