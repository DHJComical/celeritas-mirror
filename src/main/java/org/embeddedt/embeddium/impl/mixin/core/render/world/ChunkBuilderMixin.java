package org.embeddedt.embeddium.impl.mixin.core.render.world;

//? if <1.20.2 {
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
//?} else {
/*import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
*///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(SectionRenderDispatcher.class)
public class ChunkBuilderMixin {
    @ModifyVariable(method =
            "<init>(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/client/renderer/LevelRenderer;Ljava/util/concurrent/Executor;Z" +
                    /*? if <1.20.2 {*/
                    "Lnet/minecraft/client/renderer/ChunkBufferBuilderPack;"
                    /*?} else {*/
                    /*"Lnet/minecraft/client/renderer/SectionBufferBuilderPack;"*//*?}*/
                    /*? if forgelike {*/ + "I"/*?}*/ + ")V", index = 9, at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Lists;newArrayListWithExpectedSize(I)Ljava/util/ArrayList;", remap = false))
    private int modifyThreadPoolSize(int prev) {
        // Do not allow any resources to be allocated
        return 0;
    }
}
