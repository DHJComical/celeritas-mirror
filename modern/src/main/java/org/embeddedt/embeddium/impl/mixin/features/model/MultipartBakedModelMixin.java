package org.embeddedt.embeddium.impl.mixin.features.model;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
//? if forgelike && >=1.19 {
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
//?}
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.MultiPartBakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
//$ rng_import
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
//? if forge && >=1.19 {
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.data.ModelData;
//?}
//? if forge && <1.19 {
/*import net.minecraftforge.client.model.data.IModelData;
*///?}
//? if forge && >=1.16
import net.minecraftforge.client.model.data.MultipartModelData;
//? if neoforge {
/*import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.MultipartModelData;
*///?}

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Predicate;

@Mixin(MultiPartBakedModel.class)
public class MultipartBakedModelMixin {
    @Unique
    private final Map<BlockState,
            //? if <1.21.5-alpha.25.7.a {
            net.minecraft.client.resources.model.BakedModel[]
            //?} else
            /*net.minecraft.client.renderer.block.model.BlockStateModel[]*/
            > stateCacheFast = new Reference2ReferenceOpenHashMap<>();
    @Unique
    private final StampedLock lock = new StampedLock();

    //? if <1.21.2 {
    @Shadow
    @Final
    private List<Pair<Predicate<BlockState>, net.minecraft.client.resources.model.BakedModel>> selectors;

    @Unique
    private static Predicate<BlockState> embeddium$getCond(Pair<Predicate<BlockState>, net.minecraft.client.resources.model.BakedModel> selector) {
        return selector.getLeft();
    }

    @Unique
    private static net.minecraft.client.resources.model.BakedModel embeddium$getModel(Pair<Predicate<BlockState>, net.minecraft.client.resources.model.BakedModel> selector) {
        return selector.getRight();
    }
    //?} else {
    /*@Shadow
    @Final
    private List<MultiPartBakedModel.Selector> selectors;

    @Unique
    private static Predicate<BlockState> embeddium$getCond(MultiPartBakedModel.Selector selector) {
        return selector.condition();
    }

    @Unique
    //? if <1.21.5-alpha.25.7.a {
    private static net.minecraft.client.resources.model.BakedModel embeddium$getModel(MultiPartBakedModel.Selector selector) { return selector.model(); }
    //?} else {
    /^private static net.minecraft.client.renderer.block.model.BlockStateModel embeddium$getModel(MultiPartBakedModel.Selector selector) { return selector.model(); }
    ^///?}
    *///?}



    //? if forgelike && >=1.19 {
    @Unique
    private boolean embeddium$hasCustomRenderTypes;

    /**
     * @author embeddedt
     * @reason Forge allows the submodels to specify differing render type sets. As such, the parent model has
     * to return a union of all the render types the submodels want to render. That means the multipart's getQuads will
     * be called with render types which not all submodels may want. To solve this, Forge calls getRenderTypes again
     * inside getQuads, and suppresses the nested getQuads call for render types that aren't important. This sounds
     * good on paper, but most vanilla models will just delegate to {@link ItemBlockRenderTypes} for getRenderTypes,
     * and that is not fast as it requires two hashmap lookups. In total, each submodel will have its render types
     * queried around 8 times.
     * <p></p>
     * The solution? Vanilla multiparts will just have SimpleBakedModel instances inside, all with no render type
     * override. We can detect this and skip the unnecessary work, since in that case all the models will share
     * the same render type set, and thus not need any filtering.
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void checkSubModelRenderTypes(CallbackInfo ci) {
        boolean hasRenderTypes = false;
        for (var pair : selectors) {
            var model = embeddium$getModel(pair);
            // Check for the exact class in case someone extends SimpleBakedModel
            if (model.getClass() == net.minecraft.client.resources.model.SimpleBakedModel.class) {
                // SimpleBakedModel delegates to ItemBlockRenderTypes unless there is an explicit override
                hasRenderTypes = ((SimpleBakedModelAccessor)model).getBlockRenderTypes() != null;
            } else {
                // Assume any other model needs to have getRenderTypes() called
                hasRenderTypes = true;
            }
            if (hasRenderTypes) {
                break;
            }
        }
        this.embeddium$hasCustomRenderTypes = hasRenderTypes;
    }

    //?}

    //? if <1.21.5-alpha.25.7.a {
    @Unique
    private net.minecraft.client.resources.model.BakedModel[] getModelComponents(BlockState state) {
        net.minecraft.client.resources.model.BakedModel[] models;
    //?} else {
    /*@Unique
    private net.minecraft.client.renderer.block.model.BlockStateModel[] getModelComponents(BlockState state) {
        net.minecraft.client.renderer.block.model.BlockStateModel[] models;
    *///?}

        long readStamp = this.lock.readLock();
        try {
            models = this.stateCacheFast.get(state);
        } finally {
            this.lock.unlockRead(readStamp);
        }

        if (models == null) {
            long writeStamp = this.lock.writeLock();
            try {

                models = this.selectors.stream().filter(pair -> embeddium$getCond(pair).test(state)).map(pair -> embeddium$getModel(pair)).toArray(
                        //? if <1.21.5-alpha.25.7.a {
                        net.minecraft.client.resources.model.BakedModel[]::new
                        //?} else
                        /*net.minecraft.client.renderer.block.model.BlockStateModel[]::new*/
                );
                this.stateCacheFast.put(state, models);
            } finally {
                this.lock.unlockWrite(writeStamp);
            }
        }

        return models;
    }

    @Unique
    private static ArrayList<BakedQuad> addAllQuads(@Nullable ArrayList<BakedQuad> targetList, List<BakedQuad> incomingList) {
        if (targetList == null) {
            targetList = new ArrayList<>(incomingList);
        } else {
            int n = incomingList.size();
            targetList.ensureCapacity(targetList.size() + n);
            for (int i = 0; i < n; i++) {
                targetList.add(incomingList.get(i));
            }
        }
        return targetList;
    }

    /**
     * @author JellySquid
     * @reason Avoid expensive allocations and replace bitfield indirection
     */
    @Overwrite(/*? if forgelike && >=1.16 {*/ remap = false/*?}*/)
    public List<BakedQuad> getQuads(BlockState state, Direction face, /*$ rng >>*/ RandomSource random/*? if forgelike && >=1.19 {*/, ModelData modelData, RenderType renderLayer /*?}*//*? if forge && >=1.16 && <1.19 {*//*, IModelData modelData *//*?}*/) {
        if (state == null) {
            //? if <1.19 {
            /*// Embeddium: There needs to be Map#get() and Map#put() calls in this method in order for FerriteCore 1.18
            // and older mixins to work. This if statement is rarely hit, and the JIT should hopefully optimize away the
            // redundant call.
            //noinspection RedundantOperationOnEmptyContainer
            if(Collections.emptyMap().get(null) != null) {
                // This must be a local so that the put() call is an interface dispatch instead of being invoked
                // on HashMap directly
                Map<Object, Object> fakeMap = new HashMap<>();
                fakeMap.put(null, null);
            }
            *///?}
            return Collections.emptyList();
        }

        var models = getModelComponents(state);

        ArrayList<BakedQuad> quads = null;
        long seed = random.nextLong();

        //? if forgelike && >=1.19
        boolean checkSubmodelTypes = this.embeddium$hasCustomRenderTypes;

        for (var model : models) {
            random.setSeed(seed);

            // Embeddium: Filter render types as Forge does, but only if we actually need to do so. This avoids
            // the overhead of getRenderTypes() for all vanilla models. This optimization breaks mods that blindly call
            // MultiPartBakedModel#getQuads() on all render types rather than just the ones returned by getRenderTypes().
            // The original implementation accidentally handled these as a result of doing the filtering in getQuads.
            // We consider this a worthwhile tradeoff, because the API contract for chunk meshing requires iterating over
            // the return value of getRenderTypes(). To date, only Windowlogged is known to be broken by this change.
            //? if forgelike && >=1.19
            if (!checkSubmodelTypes || renderLayer == null || model.getRenderTypes(state, random, modelData).contains(renderLayer)) {
                List<BakedQuad> submodelQuads = model.getQuads(state, face, random/*? if forgelike && >=1.19 {*/, MultipartModelData.resolve(modelData, model), renderLayer/*?}*//*? if forge && >=1.16 && <1.19 {*//*, MultipartModelData.resolve(model, modelData)*//*?}*/);
                if(models.length == 1) {
                    // Nobody else will return quads, so no need to make a wrapper list
                    return submodelQuads;
                } else {
                    quads = addAllQuads(quads, submodelQuads);
                }
            //? if forgelike && >=1.19
            }
        }

        return quads != null ? quads : Collections.emptyList();
    }

    //? if forgelike && >=1.19 {
    /**
     * @author embeddedt
     * @reason faster, less allocation
     */
    @Overwrite(remap = false)
    public ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource random, @NotNull ModelData data) {
        // Consume the random value unconditionally
        long seed = random.nextLong();

        // If we know none of the submodels use custom render types, we can avoid needing to join render type sets
        // at all
        if (!this.embeddium$hasCustomRenderTypes) {
            //noinspection deprecation
            return ItemBlockRenderTypes.getRenderLayers(state);
        }

        var models = getModelComponents(state);

        if (models.length == 0) {
            return ChunkRenderTypeSet.none();
        }

        ChunkRenderTypeSet[] sets = new ChunkRenderTypeSet[models.length];

        for (int i = 0; i < models.length; i++) {
            random.setSeed(seed);
            sets[i] = models[i].getRenderTypes(state, random, data);
        }

        return ChunkRenderTypeSet.union(sets);
    }
    //?}

    //? if neoforge {
    /*/^*
     * @author embeddedt
     * @reason use our selector system, avoid creating multipart model data if no submodels use it
     ^/
    @Overwrite(remap = false)
    public ModelData getModelData(BlockAndTintGetter world, BlockPos pos, BlockState state, ModelData tileModelData) {
        var models = getModelComponents(state);

        Map<net.minecraft.client.resources.model.BakedModel, ModelData> dataMap = null;

        for(var model : models) {
            ModelData data = model.getModelData(world, pos, state, tileModelData);
            if(data != tileModelData) {
                if(dataMap == null) {
                    dataMap = new Reference2ReferenceOpenHashMap<>();
                }
                dataMap.put(model, data);
            }
        }

        return dataMap == null ? tileModelData : tileModelData.derive().with(MultipartModelDataAccessor.getProperty(), dataMap).build();
    }
    *///?}

    //? if forge && >=1.16 && <1.19 {
    /*/^*
     * @author embeddedt
     * @reason use our selector system, avoid creating multipart model data if no submodels use it
     ^/
    @Overwrite(remap = false)
    public IModelData getModelData(BlockAndTintGetter world, BlockPos pos, BlockState state, IModelData tileModelData) {
        net.minecraft.client.resources.model.BakedModel[] models = getModelComponents(state);

        Map<net.minecraft.client.resources.model.BakedModel, IModelData> dataMap = null;

        var multipartData = new MultipartModelData(tileModelData);
        boolean hadPartData = false;

        for(net.minecraft.client.resources.model.BakedModel model : models) {
            IModelData data = model.getModelData(world, pos, state, tileModelData);
            if(data != tileModelData) {
                multipartData.setPartData(model, data);
                hadPartData = true;
            }
        }

        if (!hadPartData) {
            return tileModelData;
        } else {
            return multipartData;
        }
    }
    *///?}
}