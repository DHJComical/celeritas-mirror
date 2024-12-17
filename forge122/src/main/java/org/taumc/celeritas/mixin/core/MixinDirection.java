package org.taumc.celeritas.mixin.core;

import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(EnumFacing.class)
public class MixinDirection {
    /**
     * @author <a href="mailto:skaggsm333@gmail.com">Mitchell Skaggs</a>
     * @reason Avoid looping over all directions and computing the dot product
     */
    @Overwrite
    public static EnumFacing getFacingFromVector(float x, float y, float z) {
        // Vanilla quirk: return NORTH if all entries are zero
        if (x == 0 && y == 0 && z == 0)
            return EnumFacing.NORTH;

        // First choice in ties: negative, positive; Y, Z, X
        float yM = Math.abs(y);
        float zM = Math.abs(z);
        float xM = Math.abs(x);

        if (yM >= zM) {
            if (yM >= xM) {
                // Y biggest
                if (y <= 0) {
                    return EnumFacing.DOWN;
                } else /* y > 0 */ {
                    return EnumFacing.UP;
                }
            } else /* zM <= yM < xM */ {
                // X biggest, fall through
            }
        } else /* yM < zM */ {
            if (zM >= xM) {
                // Z biggest
                if (z <= 0) {
                    return EnumFacing.NORTH;
                } else /* z > 0 */ {
                    return EnumFacing.SOUTH;
                }
            } else /* yM < zM < xM */ {
                // X biggest, fall through
            }
        }

        // X biggest
        if (x <= 0) {
            return EnumFacing.WEST;
        } else /* x > 0 */ {
            return EnumFacing.EAST;
        }
    }
}
