package org.embeddedt.embeddium.impl.mixin.features.model;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.client.resources.model.WeightedBakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
//? if >=1.21.2
/*import net.minecraft.util.random.SimpleWeightedRandomList;*/
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.world.level.block.state.BlockState;
//? if forge {
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.data.ModelData;
//?}
import org.embeddedt.embeddium.impl.model.UnwrappableBakedModel;
import org.embeddedt.embeddium.impl.util.collections.WeightedRandomListExtended;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;

import java.util.*;

@Mixin(WeightedBakedModel.class)
public class WeightedBakedModelMixin implements UnwrappableBakedModel {
    //? if <1.21.2 {
    @Shadow
    @Final
    private List<WeightedEntry.Wrapper<BakedModel>> list;

    @Shadow
    @Final
    private int totalWeight;
    //?} else {
    /*@Shadow
    @Final
    private SimpleWeightedRandomList<BakedModel> list;
    *///?}

    private static BakedModel getData(WeightedEntry.Wrapper<BakedModel> wrapper) {
        //? if >=1.21
        /*return wrapper.data();*/
        //? if <1.21
        return wrapper.getData();
    }

    /**
     * @author JellySquid
     * @reason Avoid excessive object allocations
     */
    @Overwrite(/*? if forgelike {*/ remap = false/*?}*/)
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, RandomSource random/*? if forgelike {*/, ModelData modelData, RenderType renderLayer/*?}*/) {
        //? if <1.21.2
        WeightedEntry.Wrapper<BakedModel> quad = getAt(this.list, Math.abs((int) random.nextLong()) % this.totalWeight);
        //? if >=1.21.2
        /*WeightedEntry.Wrapper<BakedModel> quad = ((WeightedRandomListExtended<WeightedEntry.Wrapper<BakedModel>>)this.list).embeddium$getRandomItem(random);*/

        if (quad != null) {
            return getData(quad)
                    .getQuads(state, face, random/*? if forgelike {*/, modelData, renderLayer/*?}*/);
        }

        return Collections.emptyList();
    }

    //? if forgelike {
    /**
     * @author embeddedt
     * @reason Avoid excessive object allocations
     */
    @Overwrite(remap = false)
    public ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data) {
        WeightedEntry.Wrapper<BakedModel> quad = getAt(this.list, Math.abs((int) rand.nextLong()) % this.totalWeight);

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

    @Override
    public @Nullable BakedModel embeddium$getInnerModel(RandomSource rand) {
        //? if <1.21.2
        WeightedEntry.Wrapper<BakedModel> quad = getAt(this.list, Math.abs((int) rand.nextLong()) % this.totalWeight);
        //? if >=1.21.2
        /*WeightedEntry.Wrapper<BakedModel> quad = ((WeightedRandomListExtended<WeightedEntry.Wrapper<BakedModel>>)this.list).embeddium$getRandomItem(rand);*/

        if (quad == null) {
            return null;
        }

        if (getData(quad).getClass() == SimpleBakedModel.class) {
            return getData(quad);
        }

        return null;
    }
}
