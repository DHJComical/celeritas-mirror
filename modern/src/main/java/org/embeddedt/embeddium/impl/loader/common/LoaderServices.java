package org.embeddedt.embeddium.impl.loader.common;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
//? if fabric
/*import org.embeddedt.embeddium.impl.loader.fabric.FabricLoaderServices;*/
//? if forgelike
import org.embeddedt.embeddium.impl.loader.forge.ForgeLoaderServices;
import org.embeddedt.embeddium.impl.model.light.LightMode;
import org.embeddedt.embeddium.impl.model.light.LightPipeline;
import org.embeddedt.embeddium.impl.model.light.data.LightDataAccess;

public interface LoaderServices {
    //? if forgelike
    LoaderServices INSTANCE = new ForgeLoaderServices();
    //? if fabric
    /*LoaderServices INSTANCE = new FabricLoaderServices();*/

    default boolean hasCustomLightPipeline() {
        return false;
    }

    default LightPipeline createCustomLightPipeline(LightMode mode, LightDataAccess cache) {
        return null;
    }

    int getFluidTintColor(BlockAndTintGetter world, FluidState state, BlockPos pos);

    default boolean isCullableAABB(AABB box) {
        return true;
    }
}
