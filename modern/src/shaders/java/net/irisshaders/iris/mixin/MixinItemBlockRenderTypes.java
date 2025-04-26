package net.irisshaders.iris.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.irisshaders.iris.shaderpack.materialmap.ModernWorldRenderingSettings;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
//? if forge
import net.minecraftforge.client.ChunkRenderTypeSet;
//? if neoforge
/*import net.neoforged.neoforge.client.ChunkRenderTypeSet;*/
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

@Mixin(ItemBlockRenderTypes.class)
public class MixinItemBlockRenderTypes {
    @ModifyExpressionValue(method = "getChunkRenderType", at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"))
    private static Object iris$setCustomRenderType(Object defaultRenderType, BlockState state) {
        Map<Block, RenderType> idMap = ModernWorldRenderingSettings.INSTANCE.getBlockTypeIds();
        if (idMap != null) {
            RenderType type = idMap.get(state.getBlock());
            return type != null ? type : defaultRenderType;
        } else {
            return defaultRenderType;
        }
    }

    //? if forgelike {
    @ModifyExpressionValue(method = "getRenderLayers", at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"))
    private static Object iris$setCustomRenderTypeForge(Object defaultRenderType, BlockState state) {
        Map<Block, RenderType> idMap = ModernWorldRenderingSettings.INSTANCE.getBlockTypeIds();
        if (idMap != null) {
            RenderType type = idMap.get(state.getBlock());
            return type != null ? ChunkRenderTypeSet.of(type) : defaultRenderType;
        } else {
            return defaultRenderType;
        }
    }
    //?}
}
