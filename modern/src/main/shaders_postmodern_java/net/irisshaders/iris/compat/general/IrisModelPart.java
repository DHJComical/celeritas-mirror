package net.irisshaders.iris.compat.general;

import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Lets a model part name the block state shaders should treat it as, such as a facade's camo. Mods implement it by
 * declaring a method with the same signature on their part class, with no dependency on Iris, so the name and
 * signature are a contract shared with upstream Iris and must not change.
 */
public interface IrisModelPart {
    default @Nullable BlockState getBlockAppearance() {
        return null;
    }
}
