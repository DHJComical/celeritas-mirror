package org.embeddedt.embeddium.impl.mixin.features.model;

//? if forge {

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
//? if >=1.19 {
import net.minecraftforge.client.model.data.ModelData;
//?} else
/*import net.minecraftforge.client.model.data.IModelData;*/
import net.minecraftforge.client.model./*? if >=1.19 {*/data./*?}*/ModelDataManager;
import org.embeddedt.embeddium.impl.model.BlockEntityImplInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(ModelDataManager.class)
public class ModelDataManagerMixin {
    //? if >=1.19 {
    @Inject(method = "requestRefresh", at = @At("HEAD"), cancellable = true)
    private void celeritas$skipIfNotModelDataProvider(BlockEntity be, CallbackInfo ci) {
        if (!BlockEntityImplInfo.providesModelData(be)) {
            ci.cancel();
        }
    }
    //?} else {
    /*@Inject(method = "requestModelDataRefresh", at = @At("HEAD"), cancellable = true)
    private static void celeritas$skipIfNotModelDataProvider(BlockEntity be, CallbackInfo ci) {
        if (!BlockEntityImplInfo.providesModelData(be)) {
            ci.cancel();
        }
    }
    *///?}

    /**
     * @author embeddedt
     * @reason Avoid inserting ModelData.EMPTY into the map, as it's redundant.
     */
    //? if >=1.19 {
    @WrapOperation(method = {
            "refreshAt"
    }, at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object celeritas$skipPuttingEmpty(Map<BlockPos, ModelData> instance, Object k, Object v, Operation<Object> original) {
        if (v == ModelData.EMPTY) {
            return instance.remove(k);
        } else {
            return original.call(instance, k, v);
        }
    }
    //?} else {
    /*@WrapOperation(method = {
            "refreshModelData"
    }, at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private static Object celeritas$skipPuttingEmpty(Map<BlockPos, IModelData> instance, Object k, Object v, Operation<Object> original) {
        if (v == net.minecraftforge.client.model.data.EmptyModelData.INSTANCE) {
            return instance.remove(k);
        } else {
            return original.call(instance, k, v);
        }
    }
    *///?}
}
//?}