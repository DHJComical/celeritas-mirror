package org.embeddedt.embeddium.impl.mixin.features.render.model.block;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = { "net/minecraft/world/level/block/state/BlockBehaviour$BlockStateBase$Cache"})
public class BlockStateCacheMixin {
    /**
     * @author embeddedt (issue originally noted by XFactHD)
     * @reason In some cases the computed face shape may be a full block but consists of multiple boxes, which prevents
     * it from being interned to Shapes.block(). Canonicalizing it here allows for the fast paths in BlockOcclusionCache
     * to be hit more frequently.
     */
    @ModifyExpressionValue(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/shapes/Shapes;getFaceShape(Lnet/minecraft/world/phys/shapes/VoxelShape;Lnet/minecraft/core/Direction;)Lnet/minecraft/world/phys/shapes/VoxelShape;"))
    private VoxelShape celeritas$normalizeFaceShape(VoxelShape shape) {
        if (shape == Shapes.empty() || shape == Shapes.block()) {
            // If the shape is already canonical, return it as-is
            return shape;
        } else if (Block.isShapeFullBlock(shape)) {
            // It's actually a full block, so canonicalize it
            return Shapes.block();
        } else {
            // Not a full block, can't do anything
            return shape;
        }
    }
}
