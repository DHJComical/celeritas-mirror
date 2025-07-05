package org.embeddedt.embeddium.impl.mixin.core.render.world;

//? if >=1.20.4 {

/*import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.SectionBufferBuilderPool;
import org.embeddedt.embeddium.impl.modern.render.chunk.NonStoringBuilderPool;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderBuffers.class)
public class ChunkBuilderMixin {
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SectionBufferBuilderPool;allocate(I)Lnet/minecraft/client/renderer/SectionBufferBuilderPool;"))
    private SectionBufferBuilderPool sodium$doNotAllocateChunks(int i) {
        return new NonStoringBuilderPool();
    }
}

*///?} else {

//? if <1.20.2 {
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
//?} else {
/*import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
*///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

//? if >=1.20.2 {
/*@Mixin(SectionRenderDispatcher.class)
*///?} else {
@Mixin(ChunkRenderDispatcher.class)
//?}
public class ChunkBuilderMixin {
    @ModifyVariable(method =
            "<init>("+
                    /*? if >=1.18 {*/ "Lnet/minecraft/client/multiplayer/ClientLevel" /*?} else {*/ /*"Lnet/minecraft/world/level/Level" *//*?}*/
                    + ";Lnet/minecraft/client/renderer/LevelRenderer;Ljava/util/concurrent/Executor;Z" +
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
// }