package net.irisshaders.iris.versionutils;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
//? if fabric {
/*import net.minecraft.client.renderer.ItemBlockRenderTypes;
*///?} else {
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
//? if neoforge {
/*import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
*///?}
//? if forge {
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.data.ModelData;
//?}
//?}

public class ModelTranslucencyHelper {
    public static boolean couldBeTranslucent(BlockState state, RandomSource randomSource) {
        //? if fabric {
        /*return ItemBlockRenderTypes.getChunkRenderType(state) == RenderType.translucent();
        *///?} else {
        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
        ChunkRenderTypeSet types = model.getRenderTypes(state, randomSource, ModelData.EMPTY);

        return types.contains(RenderType.translucent());
        //?}
    }
}
