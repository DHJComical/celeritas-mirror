package org.embeddedt.embeddium.impl.mixin.features.render.entity.sort;

//? if <1.21.6 {
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(value = LevelRenderer.class, priority = 999)
public class MixinLevelRenderer {
    @Shadow
    private ClientLevel level;

    @ModifyExpressionValue(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;entitiesForRendering()Ljava/lang/Iterable;"))
    private Iterable<Entity> batchedentityrendering$sortEntityList(Iterable<Entity> unsortedList) {
        Map<EntityType<?>, List<Entity>> sortedEntities = new HashMap<>();

        List<Entity> entities = new ArrayList<>();
        unsortedList.forEach(entity -> {
            sortedEntities.computeIfAbsent(entity.getType(), entityType -> new ArrayList<>(32)).add(entity);
        });

        sortedEntities.values().forEach(entities::addAll);

        return entities;
    }
}
//?}
