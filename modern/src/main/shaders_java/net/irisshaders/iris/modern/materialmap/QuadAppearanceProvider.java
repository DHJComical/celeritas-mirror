package net.irisshaders.iris.modern.materialmap;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.embeddedt.embeddium.impl.model.quad.BakedQuadView;
import org.embeddedt.embeddium.impl.modern.render.chunk.compile.pipeline.BlockRenderContext;
import org.jetbrains.annotations.Nullable;

/**
 * Tells which block state each quad of a block's model should take its shader ID from, for blocks whose models show
 * other blocks, such as facades. Instances belong to one meshing thread; see {@link QuadAppearanceProviders}.
 */
public interface QuadAppearanceProvider {
    /** Returns the state the quad looks like, or null to keep the block's own ID. */
    @Nullable BlockState findAppearance(BlockRenderContext ctx, @Nullable Direction side, BakedQuadView quad);

    /**
     * The shape of the part of the block the last quad passed to {@link #findAppearance} belongs to, such as one half
     * of a double block. {@link ShapeEquivalence} matches the appearance to it.
     */
    BlockState shapeState();
}
