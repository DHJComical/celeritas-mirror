package org.embeddedt.embeddium.impl.modern.render.chunk.compile.pipeline;

import it.unimi.dsi.fastutil.objects.Object2ByteLinkedOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.embeddedt.embeddium.impl.mixin.features.options.render_layers.ItemBlockRenderTypesAccessor;
import org.embeddedt.embeddium.impl.util.ModernBlockPosUtil;

/**
 * The block occlusion cache is responsible for performing occlusion testing of neighboring block faces.
 */
public class BlockOcclusionCache {
    private static final byte UNCACHED_VALUE = (byte) 127;

    private final Object2ByteLinkedOpenHashMap<CachedOcclusionShapeTest> map;
    private final CachedOcclusionShapeTest cachedTest = new CachedOcclusionShapeTest();
    private final BlockPos.MutableBlockPos cpos = new BlockPos.MutableBlockPos();

    private final boolean leavesRenderingAsSolid;

    public BlockOcclusionCache() {
        this.map = new Object2ByteLinkedOpenHashMap<>(2048, 0.5F);
        this.map.defaultReturnValue(UNCACHED_VALUE);
        this.leavesRenderingAsSolid = !ItemBlockRenderTypesAccessor.celeritas$areLeavesFancy();
    }

    private static final Direction[] OPPOSITE_CACHE = new Direction[Direction.values().length];

    static {
        for (var dir : Direction.values()) {
            OPPOSITE_CACHE[dir.ordinal()] = dir.getOpposite();
        }
    }

    /**
     * @param selfState The state of the block in the world
     * @param view The world view for this render context
     * @param pos The position of the block
     * @param facing The facing direction of the side to check
     * @return True if the block side facing {@param dir} is not occluded, otherwise false
     */
    public boolean shouldDrawSide(BlockState selfState, BlockGetter view, BlockPos pos, Direction facing) {
        // self = occluded block
        // adj = occluding block

        BlockPos.MutableBlockPos adjPos = ModernBlockPosUtil.setWithOffset(this.cpos, pos, facing);

        BlockState adjState = view.getBlockState(adjPos);

        VoxelShape selfShape, adjShape;

        Direction oppositeFacing = OPPOSITE_CACHE[facing.ordinal()];

        if (/*? if <1.21.2 {*/ adjState.canOcclude() /*?} else {*/ /*true *//*?}*/) {
            adjShape = adjState.getFaceOcclusionShape(/*? if <1.21.2 {*/view, adjPos,/*?}*/ oppositeFacing);

            // If both blocks use full-cube occlusion shapes (or we are in 1.21.2+, where only the occluding
            // block's shape is checked by vanilla), then the neighbor certainly occludes us, and we
            // shouldn't render this face.

            //? if >=1.21.2
            /*if (adjShape == Shapes.block()) return false;*/

            selfShape = selfState.getFaceOcclusionShape(/*? if <1.21.2 {*/view, pos,/*?}*/ facing);

            //? if <1.21.2
            if (adjShape == Shapes.block() && selfShape == Shapes.block()) return false;
        } else if (this.leavesRenderingAsSolid && adjState.getBlock() instanceof LeavesBlock) {
            // Allow leaves to cull like a regular solid block when in fast mode, despite not being marked as occluding
            // We use the collision shape as a way of "guessing" what the block's visual shape is, since the occlusion
            // shape might be set to empty by vanilla/mods
            adjShape = adjState.getCollisionShape(view, pos);
            selfShape = selfState.getFaceOcclusionShape(/*? if <1.21.2 {*/view, pos,/*?}*/ facing);

            if (adjShape == Shapes.block() && selfShape == Shapes.block()) return false;
        } else {
            selfShape = Shapes.empty();
            adjShape = Shapes.empty();
        }

        // We have not done a fast cull above. Start checking more specific conditions

        if (selfState.skipRendering(adjState, facing)) {
            return false;
        }

        //? if forgelike && >=1.18 {
        if (adjState.hidesNeighborFace(view, adjPos, selfState, oppositeFacing) && selfState.supportsExternalFaceHiding()) {
            // The Forge extension has requested that the face be hidden.
            return false;
        }
        //?}

        // Fast check if either shape is empty. If the occluding block does not occlude, we always end up here.
        if (selfShape == Shapes.empty() || adjShape == Shapes.empty()) {
            // If either occlusion shape is empty, then we cannot be occluded by anything, and we should render
            // this face
            return true;
        }

        // Consult the occlusion cache & do the voxel shape merging calculations if necessary
        return this.calculate(selfShape, adjShape);
    }

    private boolean calculate(VoxelShape selfShape, VoxelShape adjShape) {
        CachedOcclusionShapeTest cache = this.cachedTest;
        cache.a = selfShape;
        cache.b = adjShape;
        cache.updateHash();

        byte cached = this.map.getByte(cache);

        if (cached != UNCACHED_VALUE) {
            return cached == 1;
        }

        boolean ret = Shapes.joinIsNotEmpty(selfShape, adjShape, BooleanOp.ONLY_FIRST);

        this.map.put(cache.copy(), (byte) (ret ? 1 : 0));

        if (this.map.size() > 2048) {
            this.map.removeFirstByte();
        }

        return ret;
    }

    private static final class CachedOcclusionShapeTest {
        private VoxelShape a, b;
        private int hashCode;

        private CachedOcclusionShapeTest() {

        }

        private CachedOcclusionShapeTest(VoxelShape a, VoxelShape b, int hashCode) {
            this.a = a;
            this.b = b;
            this.hashCode = hashCode;
        }

        public void updateHash() {
            int result = System.identityHashCode(this.a);
            result = 31 * result + System.identityHashCode(this.b);

            this.hashCode = result;
        }

        public CachedOcclusionShapeTest copy() {
            return new CachedOcclusionShapeTest(this.a, this.b, this.hashCode);
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof CachedOcclusionShapeTest that) {
                return this.a == that.a &&
                        this.b == that.b;
            }

            return false;
        }

        @Override
        public int hashCode() {
            return this.hashCode;
        }
    }
}
