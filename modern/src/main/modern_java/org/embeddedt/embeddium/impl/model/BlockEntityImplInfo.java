package org.embeddedt.embeddium.impl.model;

import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

public class BlockEntityImplInfo {
    //? if forgelike {
    private static final ClassValue<Boolean> OVERRIDES_GET_MODEL_DATA = new ClassValue<>() {
        @Override
        protected Boolean computeValue(@NotNull Class<?> clz) {
            try {
                Method method = clz.getMethod("getModelData");
                //? if neoforge
                /*return method.getDeclaringClass() != net.neoforged.neoforge.common.extensions.IBlockEntityExtension.class;*/
                //? if forge && >=1.17
                return method.getDeclaringClass() != net.minecraftforge.common.extensions.IForgeBlockEntity.class;
                //? if forge && <1.17
                /*return method.getDeclaringClass() != net.minecraftforge.common.extensions.IForgeTileEntity.class;*/
            } catch (NoSuchMethodException e) {
                return false;
            }
        }
    };

    public static boolean providesModelData(BlockEntity be) {
        return OVERRIDES_GET_MODEL_DATA.get(be.getClass());
    }
    //?} else {
    /*public static boolean providesModelData(BlockEntity be) {
        return false;
    }
    *///?}
}
