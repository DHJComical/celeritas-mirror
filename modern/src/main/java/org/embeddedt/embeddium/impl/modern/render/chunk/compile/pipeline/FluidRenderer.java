package org.embeddedt.embeddium.impl.modern.render.chunk.compile.pipeline;

//? if fabric && ffapi
/*import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;*/
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.material.FlowingFluid;
import org.embeddedt.embeddium.api.render.texture.SpriteUtil;
import org.embeddedt.embeddium.api.world.EmbeddiumBlockAndTintGetter;
import org.embeddedt.embeddium.impl.model.light.LightMode;
import org.embeddedt.embeddium.impl.model.light.LightPipeline;
import org.embeddedt.embeddium.impl.model.light.LightPipelineProvider;
import org.embeddedt.embeddium.impl.model.light.data.QuadLightData;
import org.embeddedt.embeddium.impl.model.quad.ModelQuad;
import org.embeddedt.embeddium.impl.model.quad.ModelQuadView;
import org.embeddedt.embeddium.impl.model.quad.ModelQuadViewMutable;
import org.embeddedt.embeddium.impl.model.quad.ModernQuadFacing;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFlags;
import org.embeddedt.embeddium.impl.model.color.ColorProviderRegistry;
import org.embeddedt.embeddium.impl.model.color.ColorProvider;
import org.embeddedt.embeddium.impl.model.color.DefaultColorProviders;
import org.embeddedt.embeddium.impl.modern.render.chunk.ContextAwareChunkVertexEncoder;
import org.embeddedt.embeddium.impl.modern.render.chunk.MojangVertexConsumer;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildBuffers;
import org.embeddedt.embeddium.impl.modern.render.chunk.compile.ModernChunkBuildContext;
import org.embeddedt.embeddium.impl.render.chunk.compile.buffers.ChunkModelBuilder;
import org.embeddedt.embeddium.impl.render.chunk.data.MinecraftBuiltRenderSectionData;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexEncoder;
import org.embeddedt.embeddium.impl.util.DirectionUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
//? if >=1.16.2
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.apache.commons.lang3.mutable.MutableInt;
import org.embeddedt.embeddium.impl.render.chunk.compile.GlobalChunkBuildContext;
import org.embeddedt.embeddium.impl.render.chunk.ChunkColorWriter;
//? if forgelike
import org.embeddedt.embeddium.impl.render.fluid.EmbeddiumFluidSpriteCache;
//? if >=1.18
import org.embeddedt.embeddium.impl.tags.EmbeddiumTags;
import org.embeddedt.embeddium.impl.util.ModelQuadUtil;
import org.embeddedt.embeddium.impl.util.ModernBlockPosUtil;
import org.joml.Vector3fc;

import java.util.Collection;
import java.util.Objects;

/**
 * The Embeddium equivalent to vanilla's ModelBlockRenderer. It is the complement of {@link BlockRenderer} for
 * emitting fluid geometry.
 * <p>
 * This class does not need to be thread-safe, as a separate instance is allocated per meshing thread.
 */
public class FluidRenderer {
    // TODO: allow this to be changed by vertex format
    // TODO: move fluid rendering to a separate render pass and control glPolygonOffset and glDepthFunc to fix this properly
    private static final float EPSILON = 0.001f;
    private static final float ALIGNED_EQUALS_EPSILON = 0.011f;

    //? if <1.20.2 {
    private static final float SPRITE_UV_SCALING_RANGE = 16f;
    //?} else {
    /*private static final float SPRITE_UV_SCALING_RANGE = 1f;
    *///?}


    private final BlockPos.MutableBlockPos scratchPos = new BlockPos.MutableBlockPos();
    private final MutableFloat scratchHeight = new MutableFloat(0);
    private final MutableInt scratchSamples = new MutableInt();

    private final ModelQuadViewMutable quad = new ModelQuad();

    private final LightPipelineProvider lighters;

    private final QuadLightData quadLightData = new QuadLightData();
    private final int[] quadColors = new int[4];

    private final ChunkVertexEncoder.Vertex[] vertices = ChunkVertexEncoder.Vertex.uninitializedQuad();
    private final ColorProviderRegistry colorProviderRegistry;

    //? if forgelike
    private final EmbeddiumFluidSpriteCache fluidSpriteCache = new EmbeddiumFluidSpriteCache();

    private final ChunkColorWriter colorEncoder = ChunkColorWriter.get();
    private final MojangVertexConsumer vertexConsumer = new MojangVertexConsumer();

    //? if fabric && ffapi && >=1.17
    /*private final FabricFluidRenderer fabricFluidRenderer = new FabricFluidRenderer();*/

    /**
     * Whether any fluids exist with the RENDERS_WITH_VANILLA tag. This allows the check to be elided most of the time.
     */
    private final boolean doVanillaRenderedFluidsExist;

    private final TextureAtlasSprite[] lavaSprites;
    private final TextureAtlasSprite[] waterSprites;

    public FluidRenderer(ColorProviderRegistry colorProviderRegistry, LightPipelineProvider lighters) {
        this.quad.setLightFace(Direction.UP);

        this.lighters = lighters;
        this.colorProviderRegistry = colorProviderRegistry;

        this.lavaSprites = new TextureAtlasSprite[2];
        this.lavaSprites[0] = Minecraft.getInstance().getModelManager().getBlockModelShaper().getBlockModel(Blocks.LAVA.defaultBlockState())
                /*? if >=1.21.5-alpha.25.7.a {*//*.particleIcon()*//*?} else {*/.getParticleIcon()/*?}*/;
        this.lavaSprites[1] = ModelBakery.LAVA_FLOW.sprite();
        this.waterSprites = new TextureAtlasSprite[3];
        this.waterSprites[0] = Minecraft.getInstance().getModelManager().getBlockModelShaper().getBlockModel(Blocks.WATER.defaultBlockState())
                /*? if >=1.21.5-alpha.25.7.a {*//*.particleIcon()*//*?} else {*/.getParticleIcon()/*?}*/;
        this.waterSprites[1] = ModelBakery.WATER_FLOW.sprite();
        this.waterSprites[2] = ModelBakery.WATER_OVERLAY.sprite();

        //? if >=1.20 {
        this.doVanillaRenderedFluidsExist = net.minecraft.core.registries.BuiltInRegistries.FLUID.getTagOrEmpty(EmbeddiumTags.RENDERS_WITH_VANILLA).iterator().hasNext();
        //?} else
        /*this.doVanillaRenderedFluidsExist = false;*/
    }

    /**
     * {@return true if a fluid's face is occluded by surrounding block/fluid geometry and thus does not need to be rendered}
     * @param world the block getter that can be used to obtain more context about surrounding blocks
     * @param x the X coordinate of the current fluid
     * @param y the Y coordinate of the current fluid
     * @param z the Z coordinate of the current fluid
     * @param dir the face to check for occlusion on
     * @param fluid the type of the current fluid
     */
    private boolean isFluidOccluded(BlockAndTintGetter world, int x, int y, int z, Direction dir, Fluid fluid) {
        // Check if the fluid adjacent to us in the given direction is the same
        if (world.getFluidState(this.scratchPos.set(x + dir.getStepX(), y + dir.getStepY(), z + dir.getStepZ())).getType().isSame(fluid)) {
            return true;
        }

        // Stricter than vanilla: check whether the containing block can occlude, has a sturdy face on the given side,
        // and has a solid occlusion shape. If so, assume the fluid inside is not visible on that side.
        // This avoids rendering the top face of water inside an upper waterlogged slab, for instance.
        BlockPos pos = this.scratchPos.set(x, y, z);
        BlockState blockState = world.getBlockState(pos);

        if (!blockState.canOcclude() || !blockState.isFaceSturdy(world, pos, dir/*? if >=1.16.2 {*/, SupportType.FULL/*?}*/)) {
            // The blockstate we're inside doesn't occlude or isn't sturdy on this side, so it cannot possibly
            // be hiding the fluid
            return false;
        }

        VoxelShape sideShape = blockState.getFaceOcclusionShape(/*? if <1.21.2 {*/world, pos,/*?}*/ dir);
        if (sideShape == Shapes.block()) {
            // The face fills the 1x1 area, so the fluid is occluded
            return true;
        } else if (sideShape == Shapes.empty()) {
            // The face does not exist, so the fluid is not occluded
            return false;
        } else {
            // Check if the face fills the 1x1 area
            return Block.isShapeFullBlock(sideShape);
        }
    }

    private boolean isSideExposed(BlockAndTintGetter world, int x, int y, int z, Direction dir, float height) {
        BlockPos pos = this.scratchPos.set(x + dir.getStepX(), y + dir.getStepY(), z + dir.getStepZ());
        BlockState blockState = world.getBlockState(pos);

        if (blockState.canOcclude()) {
            VoxelShape shape = blockState.getOcclusionShape(/*? if <1.21.2 {*/world, pos/*?}*/);

            // Hoist these checks to avoid allocating the shape below
            if (shape == Shapes.block()) {
                // The top face always be inset, so if the shape above is a full cube it can't possibly occlude
                return dir == Direction.UP;
            } else if (shape.isEmpty()) {
                return true;
            }

            VoxelShape threshold = Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, height, 1.0D);

            //? if <=1.21.4 {
            return !Shapes.blockOccudes(threshold, shape, dir);
            //?} else
            /*return !Shapes.blockOccludes(threshold, shape, dir);*/
        }

        return true;
    }

    private static boolean isAlignedEquals(float a, float b) {
        return Math.abs(a - b) <= ALIGNED_EQUALS_EPSILON;
    }

    private void renderVanilla(EmbeddiumBlockAndTintGetter world, FluidState fluidState, BlockPos blockPos, ChunkModelBuilder buffers, Material material) {
        // Call vanilla fluid renderer and capture the results
        var context = (ModernChunkBuildContext)Objects.requireNonNull(GlobalChunkBuildContext.get());
        context.setCaptureAdditionalSprites(true);
        try(var consumer = vertexConsumer.initialize(buffers, material, null)) {
            Minecraft.getInstance().getBlockRenderer().renderLiquid(blockPos, world, consumer, /*? if >=1.18 {*/ world.getBlockState(blockPos),/*?}*/ fluidState);
        }

        var sprites = context.getAdditionalCapturedSprites();

        if (buffers.getSectionContextBundle() instanceof MinecraftBuiltRenderSectionData<?,?> mcData) {
            for(TextureAtlasSprite sprite : sprites) {
                if (SpriteUtil.hasAnimation(sprite)) {
                    //noinspection unchecked
                    ((Collection<TextureAtlasSprite>)mcData.animatedSprites).add(sprite);
                }
            }
        }

        context.setCaptureAdditionalSprites(false);
    }

    /**
     * Optimized version of FluidState.getFlow that does a fast, allocation-free check first and avoids
     * calling FluidState.getFlow when not required.
     */
    private Vec3 getFlowOptimized(EmbeddiumBlockAndTintGetter world, BlockPos blockPos, FluidState fluidState) {
        var cursor = this.scratchPos;
        boolean couldBeAffected = !(fluidState.getType() instanceof FlowingFluid) || fluidState.getValue(FlowingFluid.FALLING);
        if (!couldBeAffected) {
            // Scan surrounding fluids and see if their height matches. If so, the flow is going to be zero anyway,
            // so we can avoid calling FluidState.getFlow.
            var ownHeight = fluidState.getOwnHeight();
            for (Direction direction : DirectionUtil.HORIZONTAL_DIRECTIONS) {
                ModernBlockPosUtil.setWithOffset(cursor, blockPos, direction);
                var neighborFluidState = world.getFluidState(cursor);
                var neighborHeight = neighborFluidState.getOwnHeight();
                if (neighborHeight == 0.0F || neighborHeight != ownHeight) {
                    couldBeAffected = true;
                    break;
                }
            }
        }

        if (couldBeAffected) {
            return fluidState.getFlow(world, blockPos);
        } else {
            return Vec3.ZERO;
        }
    }

    public void render(BlockRenderContext ctx, ChunkBuildBuffers buffers) {
        var fluidState = ctx.state().getFluidState();
        var blockPos = ctx.pos();
        var world = ctx.localSlice();
        var offset = ctx.origin();
        var material = buffers.getRenderPassConfiguration().getMaterialForRenderType(ctx.renderLayer());
        var encoder = buffers.get(material).getEncoder();
        var meshBuilder = buffers.get(material);
        Fluid fluid = fluidState.getType();

        //? if fabric && ffapi
        /*var fabricFluidHandler = FluidRenderHandlerRegistry.INSTANCE.get(fluid);*/

        //? if fabric && ffapi && >=1.17 {
        /*if (fabricFluidRenderer.renderCustomFluid(ctx, fabricFluidHandler, fluidState, buffers, material)) {
            return;
        }
        *///?}

        // Embeddium: Delegate to vanilla liquid renderer if fluid has this tag.
        //? if >=1.18 {
        if(this.doVanillaRenderedFluidsExist && fluidState.getType().is(EmbeddiumTags.RENDERS_WITH_VANILLA)) {
            renderVanilla(world, fluidState, blockPos, meshBuilder, material);
            return;
        }
        //?}

        int posX = blockPos.getX();
        int posY = blockPos.getY();
        int posZ = blockPos.getZ();

        // Each variable represents whether fluid rendering should be skipped on this side
        boolean sfUp = this.isFluidOccluded(world, posX, posY, posZ, Direction.UP, fluid);
        boolean sfDown = this.isFluidOccluded(world, posX, posY, posZ, Direction.DOWN, fluid) ||
                !this.isSideExposed(world, posX, posY, posZ, Direction.DOWN, 0.8888889F);
        boolean sfNorth = this.isFluidOccluded(world, posX, posY, posZ, Direction.NORTH, fluid);
        boolean sfSouth = this.isFluidOccluded(world, posX, posY, posZ, Direction.SOUTH, fluid);
        boolean sfWest = this.isFluidOccluded(world, posX, posY, posZ, Direction.WEST, fluid);
        boolean sfEast = this.isFluidOccluded(world, posX, posY, posZ, Direction.EAST, fluid);

        if (sfUp && sfDown && sfEast && sfWest && sfNorth && sfSouth) {
            return;
        }

        if (encoder instanceof ContextAwareChunkVertexEncoder contextAwareEncoder) {
            contextAwareEncoder.prepareToRenderFluidFace(ctx);
        }

        // LVT name kept for 1.20.1 in case a mixin captures it, the meaning of this variable is now "does the fluid
        // support AO"
        //? if forgelike && >=1.19 {
        boolean isWater = fluid.getFluidType().getLightLevel(fluidState, world, blockPos) == 0;
        //?} else if forgelike && <1.19 {
        /*boolean isWater = fluid.getAttributes().getLuminosity(world, blockPos) == 0;
        *///?} else
        /*boolean isWater = fluid.is(FluidTags.WATER);*/

        final ColorProvider<FluidState> colorProvider = this.getColorProvider(fluid);

        TextureAtlasSprite[] sprites;
        //? if forgelike {
        sprites = fluidSpriteCache.getSprites(world, blockPos, fluidState);
        //?} else if ffapi {
        /*sprites = fabricFluidHandler.getFluidSprites(world, blockPos, fluidState);
        *///?} else
        /*sprites = isWater ? this.waterSprites : this.lavaSprites;*/

        float fluidHeight = this.fluidHeight(world, fluid, blockPos, Direction.UP);
        float northWestHeight, southWestHeight, southEastHeight, northEastHeight;
        if (fluidHeight >= 1.0f) {
            northWestHeight = 1.0f;
            southWestHeight = 1.0f;
            southEastHeight = 1.0f;
            northEastHeight = 1.0f;
        } else {
            var scratchPos = this.scratchPos;
            float heightNorth = this.fluidHeight(world, fluid, ModernBlockPosUtil.setWithOffset(scratchPos, blockPos, Direction.NORTH), Direction.NORTH);
            float heightSouth = this.fluidHeight(world, fluid, ModernBlockPosUtil.setWithOffset(scratchPos, blockPos, Direction.SOUTH), Direction.SOUTH);
            float heightEast = this.fluidHeight(world, fluid, ModernBlockPosUtil.setWithOffset(scratchPos, blockPos, Direction.EAST), Direction.EAST);
            float heightWest = this.fluidHeight(world, fluid, ModernBlockPosUtil.setWithOffset(scratchPos, blockPos, Direction.WEST), Direction.WEST);
            northWestHeight = this.fluidCornerHeight(world, fluid, fluidHeight, heightNorth, heightWest, scratchPos.set(blockPos)
                    .move(Direction.NORTH)
                    .move(Direction.WEST));
            southWestHeight = this.fluidCornerHeight(world, fluid, fluidHeight, heightSouth, heightWest, scratchPos.set(blockPos)
                    .move(Direction.SOUTH)
                    .move(Direction.WEST));
            southEastHeight = this.fluidCornerHeight(world, fluid, fluidHeight, heightSouth, heightEast, scratchPos.set(blockPos)
                    .move(Direction.SOUTH)
                    .move(Direction.EAST));
            northEastHeight = this.fluidCornerHeight(world, fluid, fluidHeight, heightNorth, heightEast, scratchPos.set(blockPos)
                    .move(Direction.NORTH)
                    .move(Direction.EAST));
        }
        float yOffset = sfDown ? 0.0F : EPSILON;

        final ModelQuadViewMutable quad = this.quad;

        LightMode lightMode = isWater && Minecraft.useAmbientOcclusion() ? LightMode.SMOOTH : LightMode.FLAT;
        LightPipeline lighter = this.lighters.getLighter(lightMode);

        if (!sfUp && this.isSideExposed(world, posX, posY, posZ, Direction.UP, Math.min(Math.min(northWestHeight, southWestHeight), Math.min(southEastHeight, northEastHeight)))) {
            northWestHeight -= EPSILON;
            southWestHeight -= EPSILON;
            southEastHeight -= EPSILON;
            northEastHeight -= EPSILON;

            Vec3 velocity = getFlowOptimized(world, blockPos, fluidState);

            TextureAtlasSprite sprite;
            ModelQuadFacing facing;
            float u1, u2, u3, u4;
            float v1, v2, v3, v4;

            int shapeFlags;

            if (velocity.x == 0.0D && velocity.z == 0.0D) {
                sprite = sprites[0];
                facing = ModelQuadFacing.POS_Y;
                u1 = sprite.getU(0);
                v1 = sprite.getV(0);
                u2 = u1;
                v2 = sprite.getV(SPRITE_UV_SCALING_RANGE);
                u3 = sprite.getU(SPRITE_UV_SCALING_RANGE);
                v3 = v2;
                u4 = u3;
                v4 = v1;

                shapeFlags = ModelQuadFlags.IS_PARALLEL;
            } else {
                sprite = sprites[1];
                facing = ModelQuadFacing.UNASSIGNED;
                float dir = (float) Mth.atan2(velocity.z, velocity.x) - (1.5707964f);
                float sin = Mth.sin(dir) * 0.25F;
                float cos = Mth.cos(dir) * 0.25F;
                u1 = sprite.getU((SPRITE_UV_SCALING_RANGE/2) + (-cos - sin) * SPRITE_UV_SCALING_RANGE);
                v1 = sprite.getV((SPRITE_UV_SCALING_RANGE/2) + (-cos + sin) * SPRITE_UV_SCALING_RANGE);
                u2 = sprite.getU((SPRITE_UV_SCALING_RANGE/2) + (-cos + sin) * SPRITE_UV_SCALING_RANGE);
                v2 = sprite.getV((SPRITE_UV_SCALING_RANGE/2) + (cos + sin) * SPRITE_UV_SCALING_RANGE);
                u3 = sprite.getU((SPRITE_UV_SCALING_RANGE/2) + (cos + sin) * SPRITE_UV_SCALING_RANGE);
                v3 = sprite.getV((SPRITE_UV_SCALING_RANGE/2) + (cos - sin) * SPRITE_UV_SCALING_RANGE);
                u4 = sprite.getU((SPRITE_UV_SCALING_RANGE/2) + (cos - sin) * SPRITE_UV_SCALING_RANGE);
                v4 = sprite.getV((SPRITE_UV_SCALING_RANGE/2) + (-cos - sin) * SPRITE_UV_SCALING_RANGE);

                shapeFlags = 0;
            }

            quad.setFlags(ModelQuadFlags.IS_VANILLA_SHADED | shapeFlags);

            float uAvg = (u1 + u2 + u3 + u4) / 4.0F;
            float vAvg = (v1 + v2 + v3 + v4) / 4.0F;
            float s3 = sprites[0].uvShrinkRatio();

            u1 = Mth.lerp(s3, u1, uAvg);
            u2 = Mth.lerp(s3, u2, uAvg);
            u3 = Mth.lerp(s3, u3, uAvg);
            u4 = Mth.lerp(s3, u4, uAvg);
            v1 = Mth.lerp(s3, v1, vAvg);
            v2 = Mth.lerp(s3, v2, vAvg);
            v3 = Mth.lerp(s3, v3, vAvg);
            v4 = Mth.lerp(s3, v4, vAvg);

            quad.setSprite(sprite);

            // top surface alignedness is calculated with a more relaxed epsilon
            boolean aligned = isAlignedEquals(northEastHeight, northWestHeight)
                    && isAlignedEquals(northWestHeight, southEastHeight)
                    && isAlignedEquals(southEastHeight, southWestHeight)
                    && isAlignedEquals(southWestHeight, northEastHeight);

            boolean creaseNorthEastSouthWest = aligned
                    || northEastHeight > northWestHeight && northEastHeight > southEastHeight
                    || northEastHeight < northWestHeight && northEastHeight < southEastHeight
                    || southWestHeight > northWestHeight && southWestHeight > southEastHeight
                    || southWestHeight < northWestHeight && southWestHeight < southEastHeight;

            if (creaseNorthEastSouthWest) {
                setVertex(quad, 1, 0.0f, northWestHeight, 0.0f, u1, v1);
                setVertex(quad, 2, 0.0f, southWestHeight, 1.0F, u2, v2);
                setVertex(quad, 3, 1.0F, southEastHeight, 1.0F, u3, v3);
                setVertex(quad, 0, 1.0F, northEastHeight, 0.0f, u4, v4);
            } else {
                setVertex(quad, 0, 0.0f, northWestHeight, 0.0f, u1, v1);
                setVertex(quad, 1, 0.0f, southWestHeight, 1.0F, u2, v2);
                setVertex(quad, 2, 1.0F, southEastHeight, 1.0F, u3, v3);
                setVertex(quad, 3, 1.0F, northEastHeight, 0.0f, u4, v4);
            }

            this.updateQuad(quad, world, blockPos, lighter, Direction.UP, 1.0F, colorProvider, fluidState);
            this.writeQuad(meshBuilder, material, offset, quad, facing, false, ctx);

            if (fluidState.shouldRenderBackwardUpFace(world, this.scratchPos.set(posX, posY + 1, posZ))) {
                this.writeQuad(meshBuilder, material, offset, quad,
                        ModelQuadFacing.NEG_Y, true, ctx);

            }

        }

        if (!sfDown) {
            TextureAtlasSprite sprite = sprites[0];

            float minU = sprite.getU0();
            float maxU = sprite.getU1();
            float minV = sprite.getV0();
            float maxV = sprite.getV1();
            quad.setSprite(sprite);

            setVertex(quad, 0, 0.0f, yOffset, 1.0F, minU, maxV);
            setVertex(quad, 1, 0.0f, yOffset, 0.0f, minU, minV);
            setVertex(quad, 2, 1.0F, yOffset, 0.0f, maxU, minV);
            setVertex(quad, 3, 1.0F, yOffset, 1.0F, maxU, maxV);

            quad.setFlags(ModelQuadFlags.IS_VANILLA_SHADED | ModelQuadFlags.IS_PARALLEL);

            this.updateQuad(quad, world, blockPos, lighter, Direction.DOWN, 1.0F, colorProvider, fluidState);
            this.writeQuad(meshBuilder, material, offset, quad, ModelQuadFacing.NEG_Y, false, ctx);

        }

        quad.setFlags(ModelQuadFlags.IS_VANILLA_SHADED | ModelQuadFlags.IS_PARALLEL | ModelQuadFlags.IS_ALIGNED);

        for (Direction dir : DirectionUtil.HORIZONTAL_DIRECTIONS) {
            float c1;
            float c2;
            float x1;
            float z1;
            float x2;
            float z2;

            switch (dir) {
                case NORTH -> {
                    if (sfNorth) {
                        continue;
                    }
                    c1 = northWestHeight;
                    c2 = northEastHeight;
                    x1 = 0.0f;
                    x2 = 1.0F;
                    z1 = EPSILON;
                    z2 = z1;
                }
                case SOUTH -> {
                    if (sfSouth) {
                        continue;
                    }
                    c1 = southEastHeight;
                    c2 = southWestHeight;
                    x1 = 1.0F;
                    x2 = 0.0f;
                    z1 = 1.0f - EPSILON;
                    z2 = z1;
                }
                case WEST -> {
                    if (sfWest) {
                        continue;
                    }
                    c1 = southWestHeight;
                    c2 = northWestHeight;
                    x1 = EPSILON;
                    x2 = x1;
                    z1 = 1.0F;
                    z2 = 0.0f;
                }
                case EAST -> {
                    if (sfEast) {
                        continue;
                    }
                    c1 = northEastHeight;
                    c2 = southEastHeight;
                    x1 = 1.0f - EPSILON;
                    x2 = x1;
                    z1 = 0.0f;
                    z2 = 1.0F;
                }
                default -> {
                    continue;
                }
            }

            if (this.isSideExposed(world, posX, posY, posZ, dir, Math.max(c1, c2))) {
                int adjX = posX + dir.getStepX();
                int adjY = posY + dir.getStepY();
                int adjZ = posZ + dir.getStepZ();

                TextureAtlasSprite sprite = sprites[1];

                boolean isOverlay = false;

                if (sprites.length > 2) {
                    BlockPos adjPos = this.scratchPos.set(adjX, adjY, adjZ);
                    BlockState adjBlock = world.getBlockState(adjPos);

                    if (sprites[2] != null &&
                            /*? if forgelike {*/
                            adjBlock.shouldDisplayFluidOverlay(world, adjPos, fluidState)
                            /*?} else if ffapi && >=1.18 {*/
                            /*FluidRenderHandlerRegistry.INSTANCE.isBlockTransparent(adjBlock.getBlock())
                            *//*?} else {*/
                            /*adjBlock.getBlock() instanceof HalfTransparentBlock || adjBlock.getBlock() instanceof LeavesBlock
                            *//*?}*/) {
                        sprite = sprites[2];
                        isOverlay = true;
                    }
                }

                float u1 = sprite.getU(0.0F);
                float u2 = sprite.getU(SPRITE_UV_SCALING_RANGE / 2);
                float v1 = sprite.getV((1.0F - c1) * SPRITE_UV_SCALING_RANGE * 0.5F);
                float v2 = sprite.getV((1.0F - c2) * SPRITE_UV_SCALING_RANGE * 0.5F);
                float v3 = sprite.getV(SPRITE_UV_SCALING_RANGE / 2);

                quad.setSprite(sprite);

                setVertex(quad, 0, x2, c2, z2, u2, v2);
                setVertex(quad, 1, x2, yOffset, z2, u2, v3);
                setVertex(quad, 2, x1, yOffset, z1, u1, v3);
                setVertex(quad, 3, x1, c1, z1, u1, v1);

                float br;

                if (WorldRenderingSettings.INSTANCE.shouldDisableDirectionalShading()) {
                    br = 1.0f;
                } else if (dir.getAxis() == Direction.Axis.Z) {
                    br = 0.8f;
                } else {
                    br = 0.6f;
                }

                ModelQuadFacing facing = ModernQuadFacing.fromDirection(dir);

                this.updateQuad(quad, world, blockPos, lighter, dir, br, colorProvider, fluidState);
                this.writeQuad(meshBuilder, material, offset, quad, facing, false, ctx);

                if (!isOverlay) {
                    this.writeQuad(meshBuilder, material, offset, quad, facing.getOpposite(), true, ctx);
                }

            }
        }

        if (encoder instanceof ContextAwareChunkVertexEncoder contextAwareEncoder) {
            contextAwareEncoder.finishRenderingBlock();
        }
    }

    private ColorProvider<FluidState> getColorProvider(Fluid fluid) {
        var override = this.colorProviderRegistry.getColorProvider(fluid);

        if (override != null) {
            return override;
        }
        
        return DefaultColorProviders.getFluidProvider();
    }

    private void updateQuad(ModelQuadView quad, EmbeddiumBlockAndTintGetter world, BlockPos pos, LightPipeline lighter, Direction dir, float brightness,
                            ColorProvider<FluidState> colorProvider, FluidState fluidState) {
        QuadLightData light = this.quadLightData;
        lighter.calculate(quad, pos, light, null, dir, false);

        colorProvider.getColors(world, pos, fluidState, quad, this.quadColors);

        // multiply the per-vertex color against the combined brightness
        // the combined brightness is the per-vertex brightness multiplied by the block's brightness
        for (int i = 0; i < 4; i++) {
            this.quadColors[i] = colorEncoder.writeColor(this.quadColors[i], light.br[i] * brightness);
        }
    }

    private static final int VANILLA_FLUID_NORMAL = DirectionUtil.PACKED_NORMALS[Direction.UP.ordinal()];

    private void writeQuad(ChunkModelBuilder builder, Material material, Vector3fc offset, ModelQuadView quad,
                           ModelQuadFacing facing, boolean flip, BlockRenderContext ctx) {
        var vertices = this.vertices;

        int trueNormal = facing != ModelQuadFacing.UNASSIGNED ? facing.getPackedNormal() : ModelQuadUtil.calculateNormal(quad);

        for (int i = 0; i < 4; i++) {
            var out = vertices[flip ? (3 - i + 1) & 0b11 : i];
            out.x = offset.x() + quad.getX(i);
            out.y = offset.y() + quad.getY(i);
            out.z = offset.z() + quad.getZ(i);

            out.color = this.quadColors[i];
            out.u = quad.getTexU(i);
            out.v = quad.getTexV(i);
            out.light = this.quadLightData.lm[i];

            out.vanillaNormal = VANILLA_FLUID_NORMAL;
            out.trueNormal = trueNormal;
        }

        TextureAtlasSprite sprite = quad.getSprite();

        if (SpriteUtil.hasAnimation(sprite) && builder.getSectionContextBundle() instanceof MinecraftBuiltRenderSectionData<?,?> mcData) {
            //noinspection unchecked
            ((Collection<TextureAtlasSprite>)mcData.animatedSprites).add(sprite);
        }

        var vertexBuffer = builder.getVertexBuffer(facing);
        vertexBuffer.push(vertices, material);
    }

    private static void setVertex(ModelQuadViewMutable quad, int i, float x, float y, float z, float u, float v) {
        quad.setX(i, x);
        quad.setY(i, y);
        quad.setZ(i, z);
        quad.setTexU(i, u);
        quad.setTexV(i, v);
    }

    private float fluidCornerHeight(BlockAndTintGetter world, Fluid fluid, float fluidHeight, float fluidHeightX, float fluidHeightY, BlockPos blockPos) {
        if (fluidHeightY >= 1.0f || fluidHeightX >= 1.0f) {
            return 1.0f;
        }

        if (fluidHeightY > 0.0f || fluidHeightX > 0.0f) {
            float height = this.fluidHeight(world, fluid, blockPos, Direction.UP);

            if (height >= 1.0f) {
                return 1.0f;
            }

            this.modifyHeight(this.scratchHeight, this.scratchSamples, height);
        }

        this.modifyHeight(this.scratchHeight, this.scratchSamples, fluidHeight);
        this.modifyHeight(this.scratchHeight, this.scratchSamples, fluidHeightY);
        this.modifyHeight(this.scratchHeight, this.scratchSamples, fluidHeightX);

        float result = this.scratchHeight.floatValue() / this.scratchSamples.intValue();
        this.scratchHeight.setValue(0);
        this.scratchSamples.setValue(0);

        return result;
    }

    private void modifyHeight(MutableFloat totalHeight, MutableInt samples, float target) {
        if (target >= 0.8f) {
            totalHeight.add(target * 10.0f);
            samples.add(10);
        } else if (target >= 0.0f) {
            totalHeight.add(target);
            samples.increment();
        }
    }

    private float fluidHeight(BlockAndTintGetter world, Fluid fluid, BlockPos blockPos, Direction direction) {
        BlockState blockState = world.getBlockState(blockPos);
        FluidState fluidState = blockState.getFluidState();

        if (fluid.isSame(fluidState.getType())) {
            FluidState fluidStateUp = world.getFluidState(blockPos.above());

            if (fluid.isSame(fluidStateUp.getType())) {
                return 1.0f;
            } else {
                return fluidState.getOwnHeight();
            }
        }
        if (!blockState/*? if <1.20 {*//*.getMaterial()*//*?}*/.isSolid()) {
            return 0.0f;
        }
        return -1.0f;
    }
}
