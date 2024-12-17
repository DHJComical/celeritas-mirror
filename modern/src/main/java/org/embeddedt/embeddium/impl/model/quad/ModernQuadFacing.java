package org.embeddedt.embeddium.impl.model.quad;

import net.minecraft.core.Direction;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;

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
}
