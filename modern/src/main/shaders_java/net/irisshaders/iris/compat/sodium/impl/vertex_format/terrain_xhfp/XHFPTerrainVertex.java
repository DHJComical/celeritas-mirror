package net.irisshaders.iris.compat.sodium.impl.vertex_format.terrain_xhfp;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
//? if >=26.1
//import net.irisshaders.iris.compat.general.IrisModelPart;
import net.irisshaders.iris.modern.materialmap.QuadAppearanceProvider;
import net.irisshaders.iris.modern.materialmap.QuadAppearanceProviders;
import net.irisshaders.iris.modern.materialmap.ShapeEquivalence;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.embeddedt.embeddium.impl.model.quad.BakedQuadView;
import org.embeddedt.embeddium.impl.modern.render.chunk.ContextAwareChunkVertexEncoder;
import org.embeddedt.embeddium.impl.modern.render.chunk.compile.pipeline.BlockRenderContext;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexEncoder;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.impl.QuadRelativeChunkVertex;
import org.jetbrains.annotations.Nullable;
import org.taumc.celeritas.shaders.CeleritasShaders;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class XHFPTerrainVertex implements ChunkVertexEncoder, ContextAwareChunkVertexEncoder {



    private int localPosX;
    private int localPosY;
    private int localPosZ;

    private short blockId;
    private short renderType;
    private boolean ignoreMidBlock;
    private byte lightValue;

    private final Object2IntMap<BlockState> blockStateIds = Objects.requireNonNullElse(WorldRenderingSettings.INSTANCE.getBlockStateIds(), Object2IntMaps.emptyMap());

    private final @Nullable ShapeEquivalence shapeEquivalence = WorldRenderingSettings.INSTANCE.getShapeEquivalence();
    private final ShapeEquivalence.Probe shapeProbe = new ShapeEquivalence.Probe();

    private final QuadAppearanceProviders.Lookup appearanceProviders = QuadAppearanceProviders.createLookup();

    private BlockState appearanceShape;

    // The last appearance resolved. Every quad of a block asks, nearly always with the same result.
    private BlockState memoShapeState;
    private BlockState memoAppearanceState;
    private short memoBlockId;

    private final QuadRelativeChunkVertex.Encoder baseEncoder = XHFPModelVertexType.BASE_VERTEX_TYPE.createEncoder();

    private static final ClassValue<AtomicBoolean> GET_APPEARANCE_CRASHED = new ClassValue<>() {
        @Override
        protected AtomicBoolean computeValue(Class<?> blockClass) {
            return new AtomicBoolean(false);
        }
    };

    private void setLocalPos(BlockRenderContext ctx) {
        var pos = ctx.pos();
        this.localPosX = pos.getX();
        this.localPosY = pos.getY();
        this.localPosZ = pos.getZ();
    }

    private static void logGetAppearanceFailure(BlockState state, Exception e) {
        if (GET_APPEARANCE_CRASHED.get(state.getBlock().getClass()).compareAndSet(false, true)) {
            CeleritasShaders.logger().error("Exception retrieving block appearance for {}", state, e);
        }
    }

    @Override
    public void prepareToRenderBlockFace(BlockRenderContext ctx, @Nullable Direction side, BakedQuadView quad) {
        var state = ctx.state();
        this.blockId = (short) this.blockStateIds.getOrDefault(state, -1);
        BlockState appearanceState = this.findAppearance(ctx, state, side, quad);
        if (appearanceState != null) {
            this.applyAppearance(ctx, this.appearanceShape, appearanceState);
        }
        this.renderType = 0;
        this.lightValue = (byte)ctx.lightEmission();
        this.setLocalPos(ctx);
    }

    // Returns the state this quad looks like if it should take that state's ID, or null to keep the block's own.
    // Sets appearanceShape to the shape the quad belongs to.
    private @Nullable BlockState findAppearance(BlockRenderContext ctx, BlockState state, @Nullable Direction side, BakedQuadView quad) {
        this.appearanceShape = state;
        //? if >=26.1 {
        /*BlockState partAppearance = ((IrisModelPart) ctx.model()).getBlockAppearance();
        if (partAppearance != null && !partAppearance.isAir()) {
            return partAppearance;
        }
        *///?}
        QuadAppearanceProvider provider = this.appearanceProviders.get(state.getBlock());
        if (provider != null) {
            BlockState appearanceState = provider.findAppearance(ctx, side, quad);
            this.appearanceShape = provider.shapeState();
            return appearanceState;
        }
        //? if forgelike && >=1.19 {
        // Fallback for blocks the pack doesn't map.
        if (this.blockId == -1 && side != null) {
            try {
                BlockState appearanceState = state.getAppearance(ctx.localSlice(), ctx.pos(), side, null, null);
                if (appearanceState != state && !appearanceState.isAir()) {
                    return appearanceState;
                }
            } catch (Exception e) {
                logGetAppearanceFailure(state, e);
            }
        }
        //?}
        return null;
    }

    // Sets blockId from the state a quad of this shape and appearance resolves to.
    private void applyAppearance(BlockRenderContext ctx, BlockState shapeState, BlockState appearanceState) {
        if (shapeState == this.memoShapeState && appearanceState == this.memoAppearanceState) {
            this.blockId = this.memoBlockId;
            return;
        }

        BlockState resolved = appearanceState;
        if (this.shapeEquivalence != null) {
            resolved = this.shapeEquivalence.resolve(shapeState, appearanceState, ctx.localSlice(), ctx.pos(), this.shapeProbe);
        }
        short id = (short) this.blockStateIds.getOrDefault(resolved, -1);

        // Blocks with a dynamic shape can resolve differently by position, so the memo can't stand in for them.
        if (!shapeState.getBlock().hasDynamicShape()) {
            this.memoShapeState = shapeState;
            this.memoAppearanceState = appearanceState;
            this.memoBlockId = id;
        }

        this.blockId = id;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void prepareToVoxelizeLight(BlockState state) {
        this.blockId = (short)this.blockStateIds.getOrDefault(state, -1);
        this.renderType = 0;
        this.lightValue = (byte)state.getLightEmission();
        this.ignoreMidBlock = true;
        this.localPosX = 0;
        this.localPosY = 0;
        this.localPosZ = 0;
    }

    @Override
    public void prepareToRenderFluidFace(BlockRenderContext ctx) {
        var state = ctx.state().getFluidState().createLegacyBlock();
        this.blockId = (short) this.blockStateIds.getOrDefault(state, -1);
        this.renderType = 1;
        this.lightValue = (byte)ctx.lightEmission();
        this.setLocalPos(ctx);
    }

    @Override
    public void finishRenderingBlock() {
        this.blockId = -1;
        this.renderType = -1;
        this.localPosX = 0;
        this.localPosY = 0;
        this.localPosZ = 0;
        this.lightValue = 0;
        this.ignoreMidBlock = false;
    }

	@Override
	public long writePrimitive(long ptr, Material material, Vertex[] vertices, int chunkId) {
		short blockId = this.blockId;

		var baseEncoder = this.baseEncoder;
		baseEncoder.prepare(vertices);

		// Vertex positions are relative to the section, so only the block's position within it is needed
		int extension = encodeBlockOffset(baseEncoder.originBlock(0), localPosX)
				| (encodeBlockOffset(baseEncoder.originBlock(1), localPosY) << 2)
				| (encodeBlockOffset(baseEncoder.originBlock(2), localPosZ) << 4)
				| (ignoreMidBlock ? 0 : 1 << 6)
				| (Math.min(lightValue & 0xFF, 15) << 7)
				| ((renderType & 1) << 11)
				| ((blockId & 0xFFFF) << 12);

		return baseEncoder.writeQuad(ptr, material, vertices, chunkId, extension);
	}

	/**
	 * Clamped for quads whose minimum lies outside [-1, 3) blocks of their block's corner, which leaves their
	 * {@code at_midBlock} off by whole blocks.
	 */
	private static int encodeBlockOffset(int originBlock, int localPos) {
		return Math.max(0, Math.min(3, originBlock - (localPos & 15) + 1));
	}
}
