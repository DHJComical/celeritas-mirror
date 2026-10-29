package net.irisshaders.iris.compat.flywheel.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.impl.visualization.storage.EntityStorage;
import net.irisshaders.iris.compat.flywheel.engine.ClrwlEngine;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = EntityStorage.class, remap = false)
public abstract class EntityStorageMixin
{
    @ModifyVariable(method = "createRaw", at = @At("HEAD"), argsOnly = true)
    private VisualizationContext iris$tagContext(VisualizationContext context, @Local(argsOnly = true) Entity entity)
    {
        var entityIds = WorldRenderingSettings.INSTANCE.getEntityIds();

        if (context instanceof ClrwlEngine.ClrwlMainVisualizationContext mainCtx && entityIds != null)
        {
            var entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            return mainCtx.getEntityVisualCtx(entityIds.applyAsInt(new NamespacedId(entityId.getNamespace(), entityId.getPath())));
        }

        return context;
    }
}
