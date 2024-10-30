package org.embeddedt.embeddium.impl.mixin.features.model;

//? if forge {
import net.minecraftforge.client.extensions.IForgeBakedModel;
import net.minecraftforge.client.model.data.ModelData;
//?} else if neoforge {
/*import net.neoforged.neoforge.client.extensions.IBakedModelExtension;
import net.neoforged.neoforge.client.model.data.ModelData;
*///?}

//? if forgelike {
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(value = SimpleBakedModel.class, priority = 700)
public abstract class SimpleBakedModelMixin implements /*? if forge {*/ IForgeBakedModel /*?} else {*/ /*IBakedModelExtension *//*?}*/ {
    @Shadow
    public abstract List<BakedQuad> getQuads(@Nullable BlockState pState, @Nullable Direction pDirection, RandomSource pRandom);

    /**
     * @author embeddedt
     * @reason avoid interface dispatch on getQuads() from our block renderer
     */
    @Intrinsic
    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData data, @Nullable RenderType renderType) {
        return this.getQuads(state, side, rand);
    }
}
//?}