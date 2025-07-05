package org.embeddedt.embeddium.impl.mixin.core.render.world;

//? if forgelike && >=1.18 {

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(Level.class)
public abstract class LevelMixin {
    @Shadow
    @Final
    private ArrayList<BlockEntity> freshBlockEntities;

    @Shadow
    @Final
    private ArrayList<BlockEntity> pendingFreshBlockEntities;

    @Shadow
    @Final
    public boolean isClientSide;

    @Shadow
    public abstract void sendBlockUpdated(BlockPos pos, BlockState oldState, BlockState newState, int flags);

    @Inject(method = "tickBlockEntities", at = @At("HEAD"))
    private void captureBlockEntitiesForModelDataScan(CallbackInfo ci, @Share("capturedBlockEntities") LocalRef<List<BlockEntity>> listRef) {
        if (!this.isClientSide || (this.pendingFreshBlockEntities.isEmpty() && this.freshBlockEntities.isEmpty())) {
            return;
        }

        ArrayList<BlockEntity> list = new ArrayList<>();
        list.ensureCapacity(this.freshBlockEntities.size() + this.pendingFreshBlockEntities.size());
        list.addAll(this.freshBlockEntities);
        list.addAll(this.pendingFreshBlockEntities);
        listRef.set(list);
    }

    @Unique
    private static final ConcurrentHashMap<Class<? extends BlockEntity>, Boolean> OVERRIDES_GET_MODEL_DATA = new ConcurrentHashMap<>();

    /**
     * @author embeddedt
     * @reason Scan through block entities that are loaded for the first time and trigger section rebuilds if they have
     * model data.
     */
    @Inject(method = "tickBlockEntities", at = @At("RETURN"))
    private void scanBlockEntities(CallbackInfo ci, @Share("capturedBlockEntities") LocalRef<List<BlockEntity>> listRef) {
        List<BlockEntity> list = listRef.get();

        if (list == null) {
            return;
        }

        for (BlockEntity be : list) {
            // We can avoid any further checks completely on blocks without a model
            if (be.getBlockState().getRenderShape() != RenderShape.MODEL) {
                continue;
            }

            boolean overridesModelData = OVERRIDES_GET_MODEL_DATA.computeIfAbsent(be.getClass(), clz -> {
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
            if (overridesModelData) {
                // We need to trigger a section rebuild to take the newly loaded model data into account.
                // Not doing this can cause issues like Mekanism cables being disconnected when loading
                // into singleplayer for the first time.
                this.sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 2);
            }
        }
    }
}

//?}