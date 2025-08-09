package org.embeddedt.embeddium.impl.model;

import net.minecraft.world.level.block.entity.BlockEntity;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

public class BlockEntityImplInfo {
    private static final ConcurrentHashMap<Class<? extends BlockEntity>, Boolean> OVERRIDES_GET_MODEL_DATA = new ConcurrentHashMap<>();

    public static boolean providesModelData(BlockEntity be) {
        //? if forgelike {
        return OVERRIDES_GET_MODEL_DATA.computeIfAbsent(be.getClass(), clz -> {
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
        });
        //?} else
        /*return false;*/
    }
}
