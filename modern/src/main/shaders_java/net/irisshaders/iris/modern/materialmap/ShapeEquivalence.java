package net.irisshaders.iris.modern.materialmap;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static net.irisshaders.iris.modern.materialmap.ShapeVariant.*;

/**
 * Finds the vanilla-style block a shape made of some material looks like, so that a facade (a framed slab with a stone
 * camo) gets the ID the shader pack gives to the matching real block ({@code stone_slab}). Packs encode material and
 * shape in their IDs in their own ways, so the pack's own mapping of that block is the only reliable answer.
 */
public final class ShapeEquivalence {
    private final Object2IntMap<BlockState> blockStateIds;
    private final BlockFamilyIndex families;

    // The resolver lives until the pack's ID map changes, so the resolution cache is cleared when it reaches this size.
    private static final int MAX_RESOLVED = 16384;

    private final Map<BlockState, ShapeClass> staticShapeClasses = new ConcurrentHashMap<>();
    private final Map<Key, BlockState> resolved = new ConcurrentHashMap<>();
    private final AtomicInteger resolvedCount = new AtomicInteger();

    public ShapeEquivalence(Object2IntMap<BlockState> blockStateIds) {
        this.blockStateIds = blockStateIds;
        this.families = BlockFamilyIndex.get();
    }

    /**
     * Returns the state whose ID a block shaped like {@code shapeState} and made of {@code materialState} should use:
     * a variant of the material with the same shape that the pack maps, or the material itself if there is none.
     * {@code probe} belongs to the calling thread and is overwritten.
     */
    public BlockState resolve(BlockState shapeState, BlockState materialState, BlockGetter level, BlockPos pos, Probe probe) {
        ShapeClass shapeClass = classify(shapeState, level, pos);
        if (shapeClass == ShapeClass.FULL) {
            return materialState;
        }

        probe.set(shapeState, shapeClass, materialState);
        BlockState equivalent = resolved.get(probe);
        if (equivalent == null) {
            equivalent = findEquivalent(shapeState, shapeClass, materialState);
            if (resolved.putIfAbsent(new Key(shapeState, shapeClass, materialState), equivalent) == null
                    && resolvedCount.incrementAndGet() > MAX_RESOLVED) {
                resolved.clear();
                resolvedCount.set(0);
            }
        }
        return equivalent;
    }

    private ShapeClass classify(BlockState state, BlockGetter level, BlockPos pos) {
        // Blocks with a dynamic shape can change shape without changing state, so only the others are cached.
        if (state.getBlock().hasDynamicShape()) {
            return computeShapeClass(state, level, pos);
        }

        ShapeClass shapeClass = staticShapeClasses.get(state);
        if (shapeClass == null) {
            shapeClass = computeShapeClass(state, EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
            staticShapeClasses.putIfAbsent(state, shapeClass);
        }
        return shapeClass;
    }

    private static ShapeClass computeShapeClass(BlockState state, BlockGetter level, BlockPos pos) {
        ShapeVariant variant = ShapeVariant.of(state.getBlock());
        if (variant != null) {
            return ShapeClass.of(variant);
        }

        VoxelShape shape;
        try {
            shape = state.getShape(level, pos);
        } catch (Exception e) {
            return ShapeClass.FULL;
        }

        if (shape.isEmpty() || Block.isShapeFullBlock(shape)) {
            return ShapeClass.FULL;
        }

        double volume = 0;
        for (AABB box : shape.toAabbs()) {
            volume += clampedLength(box.minX, box.maxX) * clampedLength(box.minY, box.maxY) * clampedLength(box.minZ, box.maxZ);
        }

        // Rounded to the nearest of stairs (3/4) and slab (1/2), so stepped approximations of slopes still count.
        if (volume >= 0.625) {
            return ShapeClass.STAIRS_LIKE;
        } else if (volume >= 0.375) {
            return ShapeClass.SLAB_LIKE;
        }

        for (Direction direction : Direction.values()) {
            if (Block.isFaceFull(shape, direction)) {
                return ShapeClass.PLATE_LIKE;
            }
        }

        return ShapeClass.POST_LIKE;
    }

    private static double clampedLength(double min, double max) {
        return Math.max(0, Math.min(max, 1) - Math.max(min, 0));
    }

    private BlockState findEquivalent(BlockState shapeState, ShapeClass shapeClass, BlockState materialState) {
        Block material = materialState.getBlock();
        for (ShapeVariant shape : shapeClass.chain) {
            for (Block variant : families.getVariants(material, shape)) {
                BlockState candidate = copyProperties(shapeState, variant.defaultBlockState());
                if (blockStateIds.getOrDefault(candidate, -1) != -1) {
                    return candidate;
                }
            }
        }
        return materialState;
    }

    /** Copies every property the target also has by name, since packs key on some of them ({@code powered}). */
    public static BlockState copyProperties(BlockState from, BlockState to) {
        var definition = to.getBlock().getStateDefinition();
        for (Property<?> property : from.getProperties()) {
            Property<?> target = definition.getProperty(property.getName());
            if (target != null) {
                to = setValue(to, target, valueName(from, property));
            }
        }
        return to;
    }

    private static <T extends Comparable<T>> String valueName(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }

    private static <T extends Comparable<T>> BlockState setValue(BlockState state, Property<T> property, String valueName) {
        return property.getValue(valueName).map(value -> state.setValue(property, value)).orElse(state);
    }

    /**
     * A reusable lookup key, so cache hits don't allocate. ConcurrentHashMap compares a lookup key to stored keys with
     * the lookup key's {@code equals}, so a probe can find a {@link Key}, but it must never be stored itself.
     */
    public static final class Probe {
        private BlockState shape;
        private ShapeClass shapeClass;
        private BlockState material;
        private int hash;

        private void set(BlockState shape, ShapeClass shapeClass, BlockState material) {
            this.shape = shape;
            this.shapeClass = shapeClass;
            this.material = material;
            this.hash = Key.hash(shape, shapeClass, material);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Key key && key.shape == shape && key.shapeClass == shapeClass && key.material == material;
        }
    }

    private static final class Key {
        private final BlockState shape;
        private final ShapeClass shapeClass;
        private final BlockState material;
        private final int hash;

        Key(BlockState shape, ShapeClass shapeClass, BlockState material) {
            this.shape = shape;
            this.shapeClass = shapeClass;
            this.material = material;
            this.hash = hash(shape, shapeClass, material);
        }

        static int hash(BlockState shape, ShapeClass shapeClass, BlockState material) {
            return (shape.hashCode() * 31 + shapeClass.ordinal()) * 31 + material.hashCode();
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Key key && key.shape == shape && key.shapeClass == shapeClass && key.material == material;
        }
    }

    /** The variants to try for a shape, in order. Every chain ends at the slab, the partial block packs always map. */
    private enum ShapeClass {
        FULL(),
        STAIRS_LIKE(STAIRS, SLAB),
        SLAB_LIKE(SLAB, STAIRS),
        PLATE_LIKE(TRAPDOOR, PRESSURE_PLATE, FENCE, SLAB),
        POST_LIKE(WALL, FENCE, TRAPDOOR, PRESSURE_PLATE, BUTTON, SLAB),
        FENCE_LIKE(FENCE, WALL, TRAPDOOR, PRESSURE_PLATE, BUTTON, SLAB),
        FENCE_GATE_LIKE(FENCE_GATE, FENCE, WALL, TRAPDOOR, PRESSURE_PLATE, BUTTON, SLAB),
        DOOR_LIKE(DOOR, TRAPDOOR, PRESSURE_PLATE, FENCE, SLAB),
        BUTTON_LIKE(BUTTON, PRESSURE_PLATE, WALL, FENCE, TRAPDOOR, SLAB),
        PRESSURE_PLATE_LIKE(PRESSURE_PLATE, TRAPDOOR, FENCE, SLAB),
        PANE_LIKE(PANE, WALL, FENCE, TRAPDOOR, PRESSURE_PLATE, BUTTON, SLAB);

        final ShapeVariant[] chain;

        ShapeClass(ShapeVariant... chain) {
            this.chain = chain;
        }

        /**
         * Slabs, stairs and trapdoors share the chain of the geometric class they fall into, so a block with one of
         * those shapes that doesn't extend the vanilla class still resolves the same way.
         */
        static ShapeClass of(ShapeVariant variant) {
            return switch (variant) {
                case SLAB -> SLAB_LIKE;
                case STAIRS -> STAIRS_LIKE;
                case WALL -> POST_LIKE;
                case TRAPDOOR -> PLATE_LIKE;
                case FENCE -> FENCE_LIKE;
                case FENCE_GATE -> FENCE_GATE_LIKE;
                case DOOR -> DOOR_LIKE;
                case BUTTON -> BUTTON_LIKE;
                case PRESSURE_PLATE -> PRESSURE_PLATE_LIKE;
                case PANE -> PANE_LIKE;
            };
        }
    }
}
