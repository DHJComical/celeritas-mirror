package org.embeddedt.embeddium.impl.mixin.features.model;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.MultiPartBakedModel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.embeddedt.embeddium.impl.util.collections.map.StampedMap;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(MultiPartBakedModel.class)
public class MultipartBakedModelMixin {
    @Shadow
    @Final
    @Mutable
    private Map<BlockState, BitSet> selectorCache;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void reinitializeSelectorCacheThreadSafe(CallbackInfo ci) {
        this.selectorCache = new StampedMap<>(this.selectorCache);
    }

    //? if fabric {
    /*@Redirect(method = "getQuads", at = @At(value = "INVOKE", target = "Ljava/util/List;addAll(Ljava/util/Collection;)Z"))
    private boolean addAllWithoutAlloc(List<BakedQuad> instance, Collection<? extends BakedQuad> es) {
        if (es instanceof List<? extends BakedQuad> otherList) {
            //noinspection ForLoopReplaceableByForEach
            for (var i = 0; i < otherList.size(); i++) {
                //noinspection UseBulkOperation
                instance.add(otherList.get(i));
            }
        } else {
            instance.addAll(es);
        }
        return true;
    }
    *///?}

    //? if >=1.18 <1.21.2-alpha.24.36.a {
    @Redirect(method = "getQuads", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;create(J)Lnet/minecraft/util/RandomSource;"))
    private RandomSource reuseRandomSource(long seed, @Local(ordinal = 0, argsOnly = true) RandomSource rand) {
        rand.setSeed(seed);
        return rand;
    }
    //?}
}