package org.embeddedt.embeddium.impl.mixin.features.model;

//? if >=1.18 {
import com.google.common.collect.Iterables;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.client.resources.model.WeightedBakedModel;
import net.minecraft.core.Direction;
//$ rng_import
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.world.level.block.state.BlockState;
//? if forge && >=1.19 {
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.data.ModelData;
//?}
//? if forge && <1.19
/*import net.minecraftforge.client.model.data.IModelData;*/
//? if neoforge {
/*import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
*///?}
import org.embeddedt.embeddium.impl.util.collections.WeightedRandomListExtended;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;

import java.util.*;

@Mixin(WeightedBakedModel.class)
public class WeightedBakedModelMixin {
    @Shadow
    @Final
    private List<WeightedEntry.Wrapper<BakedModel>> list;

    @Shadow
    @Final
    private int totalWeight;

    private static BakedModel getData(WeightedEntry.Wrapper<BakedModel> wrapper) {
        //? if >=1.20.6
        /*return wrapper.data();*/
        //? if <1.20.6
        return wrapper.getData();
    }

    private WeightedEntry.Wrapper<BakedModel> embeddium$readWeightedList(/*$ rng >>*/ RandomSource random) {
        WeightedEntry.Wrapper<BakedModel> quad = getAt(this.list, Math.abs((int) random.nextLong()) % this.totalWeight);
        return quad;
    }

    /**
     * @author JellySquid
     * @reason Avoid excessive object allocations
     */
    @Overwrite(/*? if forgelike {*/ remap = false/*?}*/)
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, /*$ rng >>*/ RandomSource random/*? if forgelike && >=1.19 {*/, ModelData modelData, RenderType renderLayer/*?}*/ /*? if forgelike && <1.19 {*//*, IModelData modelData *//*?}*/) {
        var quad = embeddium$readWeightedList(random);

        if (quad != null) {
            return getData(quad)
                    .getQuads(state, face, random/*? if forgelike && >=1.19 {*/, modelData, renderLayer/*?}*//*? if forgelike && <1.19 {*//*, modelData*//*?}*/);
        }

        return Collections.emptyList();
    }

    //? if forgelike && >=1.19 {
    @Overwrite(remap = false)
    public ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data) {
        var quad = embeddium$readWeightedList(rand);

        if (quad != null) {
            return getData(quad).getRenderTypes(state, rand, data);
        }

        return ChunkRenderTypeSet.none();
    }
    //?}

    @Unique
    private static <T extends WeightedEntry> T getAt(List<T> pool, int totalWeight) {
        int i = 0;
        int len = pool.size();

        T weighted;

        do {
            if (i >= len) {
                return null;
            }

            weighted = pool.get(i++);
            totalWeight -= weighted.getWeight().asInt();
        } while (totalWeight >= 0);

        return weighted;
    }
}
//?}