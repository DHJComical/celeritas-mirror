package org.embeddedt.embeddium.impl.mixin.features.model;

//? if forge {

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelDataManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

@Mixin(ModelDataManager.class)
public class ModelDataManagerMixin {
    /**
     * @author embeddedt
     * @reason Avoid inserting ModelData.EMPTY into the map, as it's redundant.
     */
    @WrapOperation(method = "refreshAt", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object celeritas$skipPuttingEmpty(Map<BlockPos, ModelData> instance, Object k, Object v, Operation<Object> original) {
        if (v == ModelData.EMPTY) {
            return instance.remove(k);
        } else {
            return original.call(instance, k, v);
        }
    }
}
//? }