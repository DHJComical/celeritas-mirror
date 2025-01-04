package net.irisshaders.iris.mixin;

import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
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
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(ItemBlockRenderTypes.class)
public class MixinItemBlockRenderTypes {
    @Inject(method = "getChunkRenderType", at = @At("HEAD"), cancellable = true)
    private static void iris$setCustomRenderType(BlockState arg, CallbackInfoReturnable<RenderType> cir) {
        Map<Block, RenderType> idMap = WorldRenderingSettings.INSTANCE.getBlockTypeIds();
        if (idMap != null) {
            RenderType type = idMap.get(arg.getBlock());
            if (type != null) {
                cir.setReturnValue(type);
            }
        }
    }

    //? if forgelike {
	@Inject(method = "getRenderLayers", at = @At("HEAD"), cancellable = true)
	private static void iris$setCustomRenderTypeForge(BlockState arg, CallbackInfoReturnable<ChunkRenderTypeSet> cir) {
		Map<Block, RenderType> idMap = WorldRenderingSettings.INSTANCE.getBlockTypeIds();
		if (idMap != null) {
			RenderType type = idMap.get(arg.getBlock());
			if (type != null) {
				cir.setReturnValue(ChunkRenderTypeSet.of(type));
			}
		}
	}
    //?}
}
