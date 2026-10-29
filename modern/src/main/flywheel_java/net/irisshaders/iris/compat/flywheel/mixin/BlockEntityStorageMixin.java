package net.irisshaders.iris.compat.flywheel.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.impl.visualization.storage.BlockEntityStorage;
import net.irisshaders.iris.compat.flywheel.engine.ClrwlEngine;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = BlockEntityStorage.class, remap = false)
public abstract class BlockEntityStorageMixin
{
    @ModifyVariable(method = "createRaw", at = @At("HEAD"), argsOnly = true)
    private VisualizationContext iris$tagContext(VisualizationContext context, @Local(argsOnly = true) BlockEntity blockEntity)
    {
        var blockIds = WorldRenderingSettings.INSTANCE.getBlockStateIds();

        if (context instanceof ClrwlEngine.ClrwlMainVisualizationContext mainCtx && blockIds != null)
        {
            var state = blockEntity.getBlockState();
            return mainCtx.getBlockEntityVisualCtx(blockIds.getInt(state), state.getLightEmission());
        }

        return context;
    }
}
