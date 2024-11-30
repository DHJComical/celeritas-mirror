package org.embeddedt.embeddium.impl.mixin.modcompat.brandonscore;

import com.brandon3055.brandonscore.client.BCClientEventHandler;
import org.embeddedt.embeddium.impl.render.CeleritasWorldRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

import java.util.Iterator;
import java.util.Set;

@Mixin(BCClientEventHandler.class)
public class BCClientEventHandlerMixin {
    @Redirect(method = "renderLevelStage", slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;globalBlockEntities:Ljava/util/Set;", ordinal = 0)), at = @At(value = "INVOKE", target = "Ljava/util/Set;iterator()Ljava/util/Iterator;", ordinal = 0), require = 0)
    private Iterator<BlockEntity> useEmbeddiumBEIterator(Set instance) {
        return CeleritasWorldRenderer.instance().blockEntityIterator();
    }
}