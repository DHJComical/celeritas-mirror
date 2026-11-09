//? if forgelike {
package net.irisshaders.iris.compat.framedblocks;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.irisshaders.iris.modern.materialmap.QuadAppearanceProvider;
import net.irisshaders.iris.modern.materialmap.QuadAppearanceProviders;
import net.irisshaders.iris.modern.materialmap.ShapeEquivalence;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
//? if forge {
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
//?} else {
/*import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
*///?}
import org.embeddedt.embeddium.impl.model.quad.BakedQuadView;
import org.embeddedt.embeddium.impl.modern.render.chunk.compile.pipeline.BlockRenderContext;
import org.embeddedt.embeddium.impl.modern.util.ModernQuadFacing;
import org.embeddedt.embeddium.impl.util.ResourceLocationUtil;
import org.jetbrains.annotations.Nullable;
import org.taumc.celeritas.shaders.CeleritasShaders;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;

/**
 * Lets each quad of a FramedBlocks block take the ID of the camo it was made from, or of the vanilla block its
 * overlays (rails, torch heads, reinforcement) look like. FramedBlocks is only reached through method handles to its
 * public API, with no build dependency; if any member is missing, the integration stays off and framed blocks keep their
 * own IDs.
 * <p>
 * Not thread-safe: each vertex encoder owns one.
 */
public final class FramedBlocksAppearance implements QuadAppearanceProvider {
    private static final String API = "xfacthd.framedblocks.api.";
    private static final String NAMESPACE = "framedblocks";
    private static final ResourceLocation REINFORCEMENT_SPRITE = ResourceLocationUtil.make(NAMESPACE, "block/framed_reinforcement");
    private static final BlockState REINFORCEMENT_STATE = Blocks.OBSIDIAN.defaultBlockState();

    private static final Direction[] DIRECTIONS = Direction.values();
    private static final Direction.Axis[] AXES = Direction.Axis.values();
    private static final double EDGE_EPSILON = 1.0e-3;

    private static final @Nullable Handles HANDLES = Handles.find();

    private final Reference2ObjectOpenHashMap<BlockState, Overlay> overlayByFrame = new Reference2ObjectOpenHashMap<>();
    private final RandomSource random = new SingleThreadedRandomSource(42L);

    private BlockState shapeState;
    private boolean failed;

    // The camo of the last component looked at. Every quad of a block asks again, almost always about the same one.
    private long camoPos = Long.MIN_VALUE;
    private BlockState camoComponent;
    private ModelData camoModelData;
    private @Nullable BlockState camo;

    private FramedBlocksAppearance() {
    }

    public static void register() {
        Handles h = HANDLES;
        if (h != null) {
            QuadAppearanceProviders.register(block -> h.framedBlock().isInstance(block), FramedBlocksAppearance::new);
        }
    }

    @Override
    public BlockState shapeState() {
        return this.shapeState;
    }

    /**
     * Returns the vanilla block an overlay quad looks like, the camo for every other quad, or null for the frame's own
     * ID. Overlays are recognized rather than camo quads, since a frame's overlays are a fixed set while its camo
     * can be any block.
     */
    @Override
    public @Nullable BlockState findAppearance(BlockRenderContext ctx, @Nullable Direction side, BakedQuadView quad) {
        BlockState state = ctx.state();
        this.shapeState = state;
        if (this.failed) {
            return null;
        }

        TextureAtlasSprite sprite = (TextureAtlasSprite) quad.celeritas$getSprite();
        ResourceLocation spriteName = sprite.contents().name();
        if (REINFORCEMENT_SPRITE.equals(spriteName)) {
            return REINFORCEMENT_STATE;
        }

        // FramedBlocks' own textures only show on overlays once a camo is applied, or on the bare frame otherwise.
        Overlay overlay = this.overlayFor(state);
        if (spriteName.getNamespace().equals(NAMESPACE) || overlay.uses(sprite)) {
            return overlay.state();
        }

        Handles h = HANDLES;
        Block block = state.getBlock();
        BlockGetter level = ctx.localSlice();
        BlockPos pos = ctx.pos();
        try {
            Direction face = side != null ? side : ModernQuadFacing.toDirection(quad.getLightFace());
            BlockState component = (BlockState) h.getComponentAtEdge().invokeExact(block, level, pos, state, face, edgeOf(quad, face));
            if (component == null || !this.loadCamo(h, block, state, pos, ctx.modelData(), component)) {
                return null;
            }
            this.shapeState = component;
            return this.camo;
        } catch (Throwable t) {
            this.failed = true;
            CeleritasShaders.logger().error("FramedBlocks camo lookup failed, framed blocks will keep their own IDs", t);
            return null;
        }
    }

    /**
     * Returns the edge of {@code face} the quad sits against, or null if it spans the whole face. FramedBlocks uses
     * this to say which component of a double block covers that part of the face.
     */
    private static @Nullable Direction edgeOf(BakedQuadView quad, Direction face) {
        Direction edge = null;
        double edgeOffset = 0;
        for (Direction.Axis axis : AXES) {
            if (axis == face.getAxis()) {
                continue;
            }
            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < 4; i++) {
                double coord = axis.choose(quad.getX(i), quad.getY(i), quad.getZ(i));
                min = Math.min(min, coord);
                max = Math.max(max, coord);
            }
            if (min <= EDGE_EPSILON && max >= 1 - EDGE_EPSILON) {
                continue;
            }
            double offset = (min + max) * 0.5 - 0.5;
            if (Math.abs(offset) > Math.abs(edgeOffset)) {
                edgeOffset = offset;
                edge = Direction.fromAxisAndDirection(axis, offset > 0 ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
            }
        }
        return edge;
    }

    private boolean loadCamo(Handles h, Block block, BlockState state, BlockPos pos, ModelData data, BlockState component) throws Throwable {
        long packedPos = pos.asLong();
        if (packedPos == this.camoPos && component == this.camoComponent && data == this.camoModelData) {
            return this.camo != null;
        }
        this.camoPos = packedPos;
        this.camoComponent = component;
        this.camoModelData = data;

        ModelData componentData = (ModelData) h.unpackNestedModelData().invokeExact(block, data, state, component);
        Object framedData = componentData.get(h.dataProperty());
        BlockState camo = framedData != null ? (BlockState) h.getCamoState().invokeExact(framedData) : null;
        this.camo = camo != null && !camo.isAir() ? camo : null;
        return this.camo != null;
    }

    private Overlay overlayFor(BlockState frame) {
        Overlay overlay = this.overlayByFrame.get(frame);
        if (overlay == null) {
            overlay = this.computeOverlay(frame);
            this.overlayByFrame.put(frame, overlay);
        }
        return overlay;
    }

    private Overlay computeOverlay(BlockState frame) {
        var key = BuiltInRegistries.BLOCK.getKey(frame.getBlock());
        Block block = key.getNamespace().equals(NAMESPACE) ? overlayBlock(key.getPath()) : null;
        if (block == null) {
            return Overlay.NONE;
        }

        BlockState state = ShapeEquivalence.copyProperties(frame, block.defaultBlockState());
        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
        ArrayList<TextureAtlasSprite> sprites = new ArrayList<>(4);
        for (int i = 0; i <= DIRECTIONS.length; i++) {
            Direction side = i < DIRECTIONS.length ? DIRECTIONS[i] : null;
            this.random.setSeed(42L);
            for (BakedQuad quad : model.getQuads(state, side, this.random, ModelData.EMPTY, null)) {
                TextureAtlasSprite sprite = quad.getSprite();
                if (!sprites.contains(sprite)) {
                    sprites.add(sprite);
                }
            }
        }
        return new Overlay(state, sprites.toArray(new TextureAtlasSprite[0]));
    }

    /**
     * Returns the vanilla block a frame's overlays look like. Matches the overlay states FramedBlocks itself reports to
     * shaders on 1.21.5+, plus the ones it leaves out.
     */
    private static @Nullable Block overlayBlock(String path) {
        return switch (path) {
            case "framed_stone_button", "framed_large_stone_button" -> Blocks.STONE;
            case "framed_lever" -> Blocks.LEVER;
            case "framed_bookshelf" -> Blocks.BOOKSHELF;
            case "framed_chiseled_bookshelf" -> Blocks.CHISELED_BOOKSHELF;
            case "framed_hanging_sign" -> Blocks.OAK_HANGING_SIGN;
            case "framed_wall_hanging_sign" -> Blocks.OAK_WALL_HANGING_SIGN;
            case "framed_torch" -> Blocks.TORCH;
            case "framed_wall_torch" -> Blocks.WALL_TORCH;
            case "framed_soul_torch" -> Blocks.SOUL_TORCH;
            case "framed_soul_wall_torch" -> Blocks.SOUL_WALL_TORCH;
            case "framed_redstone_torch" -> Blocks.REDSTONE_TORCH;
            case "framed_redstone_wall_torch" -> Blocks.REDSTONE_WALL_TORCH;
            case "framed_flower_pot" -> Blocks.FLOWER_POT;
            case "framed_target" -> Blocks.TARGET;
            default -> path.contains("rail") ? railBlock(path) : null;
        };
    }

    private static Block railBlock(String path) {
        if (path.contains("powered")) {
            return Blocks.POWERED_RAIL;
        } else if (path.contains("detector")) {
            return Blocks.DETECTOR_RAIL;
        } else if (path.contains("activator")) {
            return Blocks.ACTIVATOR_RAIL;
        }
        return Blocks.RAIL;
    }

    /** The vanilla block a frame's overlays look like, null if none, and the sprites its model uses. */
    private record Overlay(@Nullable BlockState state, TextureAtlasSprite[] sprites) {
        static final Overlay NONE = new Overlay(null, new TextureAtlasSprite[0]);

        boolean uses(TextureAtlasSprite sprite) {
            for (TextureAtlasSprite own : this.sprites) {
                if (own == sprite) {
                    return true;
                }
            }
            return false;
        }
    }

    private record Handles(Class<?> framedBlock, MethodHandle getComponentAtEdge, MethodHandle unpackNestedModelData,
                           ModelProperty<?> dataProperty, MethodHandle getCamoState) {
        static @Nullable Handles find() {
            ClassLoader loader = FramedBlocksAppearance.class.getClassLoader();
            Class<?> framedBlock;
            try {
                framedBlock = Class.forName(API + "block.IFramedBlock", false, loader);
            } catch (ClassNotFoundException e) {
                return null;
            }

            try {
                MethodHandles.Lookup lookup = MethodHandles.publicLookup();
                Class<?> framedData = Class.forName(API + "model.data.FramedBlockData", false, loader);

                MethodHandle getComponentAtEdge = lookup.findVirtual(framedBlock, "getComponentAtEdge",
                        MethodType.methodType(BlockState.class, BlockGetter.class, BlockPos.class, BlockState.class, Direction.class, Direction.class))
                        .asType(MethodType.methodType(BlockState.class, Block.class, BlockGetter.class, BlockPos.class, BlockState.class, Direction.class, Direction.class));
                MethodHandle unpackNestedModelData = lookup.findVirtual(framedBlock, "unpackNestedModelData",
                        MethodType.methodType(ModelData.class, ModelData.class, BlockState.class, BlockState.class))
                        .asType(MethodType.methodType(ModelData.class, Block.class, ModelData.class, BlockState.class, BlockState.class));
                ModelProperty<?> dataProperty = (ModelProperty<?>) lookup.findStaticGetter(framedData, "PROPERTY", ModelProperty.class).invoke();
                MethodHandle getCamoState = findCamoState(lookup, loader, framedData)
                        .asType(MethodType.methodType(BlockState.class, Object.class));

                return new Handles(framedBlock, getComponentAtEdge, unpackNestedModelData, dataProperty, getCamoState);
            } catch (Throwable t) {
                CeleritasShaders.logger().warn("FramedBlocks' API isn't as expected, framed blocks will keep their own IDs", t);
                return null;
            }
        }

        // The camo state is on the block data in 1.20, and behind a CamoContent from 1.21.
        private static MethodHandle findCamoState(MethodHandles.Lookup lookup, ClassLoader loader, Class<?> framedData) throws ReflectiveOperationException {
            try {
                return lookup.findVirtual(framedData, "getCamoState", MethodType.methodType(BlockState.class));
            } catch (NoSuchMethodException e) {
                Class<?> camoContent = Class.forName(API + "camo.CamoContent", false, loader);
                MethodHandle getCamoContent = lookup.findVirtual(framedData, "getCamoContent", MethodType.methodType(camoContent));
                MethodHandle getAppearanceState = lookup.findVirtual(camoContent, "getAppearanceState", MethodType.methodType(BlockState.class));
                return MethodHandles.filterReturnValue(getCamoContent, getAppearanceState);
            }
        }
    }
}
//?}
