package net.irisshaders.iris.compat.sodium.impl.vertex_format.terrain_xhfp;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import net.irisshaders.iris.modern.materialmap.FallbackTextureMaterials;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.embeddedt.embeddium.impl.modern.render.chunk.ContextAwareChunkVertexEncoder;
import org.embeddedt.embeddium.impl.modern.render.chunk.compile.pipeline.BlockRenderContext;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexEncoder;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.impl.QuadRelativeChunkVertex;
//? if <26.1
import org.embeddedt.embeddium.impl.render.texture.TextureAtlasExtended;
import org.jetbrains.annotations.Nullable;
import org.taumc.celeritas.shaders.CeleritasShaders;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class XHFPTerrainVertex implements ChunkVertexEncoder, ContextAwareChunkVertexEncoder {



    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private int localPosX;
    private int localPosY;
    private int localPosZ;

    private short blockId;
    private BlockState blockState;
    private short renderType;
    private boolean ignoreMidBlock;
    private byte lightValue;

    private final Object2IntMap<BlockState> blockStateIds = Objects.requireNonNullElse(WorldRenderingSettings.INSTANCE.getBlockStateIds(), Object2IntMaps.emptyMap());

    @SuppressWarnings("deprecation")
    private final TextureAtlas blocksAtlas = (TextureAtlas)Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS);

    private final FallbackTextureMaterials fallbackMaterials = WorldRenderingSettings.INSTANCE.getFallbackTextureMaterialMapping();

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
    public void prepareToRenderBlockFace(BlockRenderContext ctx, @Nullable Direction side) {
        var state = ctx.state();
        this.blockId = (short) this.blockStateIds.getOrDefault(state, -1);
        //? if forgelike && >=1.19 {
        if (this.blockId == -1 && side != null) {
            // Attempt fallback using getAppearance. This is the plan B for block IDs; plan C will use the quad texture
            // to deduce a material.
            try {
                BlockState appearanceState = state.getAppearance(ctx.localSlice(), ctx.pos(), side, null, null);
                if (appearanceState != state && !appearanceState.isAir()) {
                    this.blockId = (short)this.blockStateIds.getOrDefault(appearanceState, -1);
                    state = appearanceState;
                }
            } catch (Exception e) {
                logGetAppearanceFailure(state, e);
            }
        }
        //?}
        this.blockState = state;
        this.renderType = 0;
        this.lightValue = (byte)ctx.lightEmission();
        this.setLocalPos(ctx);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void prepareToVoxelizeLight(BlockState state) {
        this.blockId = (short)this.blockStateIds.getOrDefault(state, -1);
        this.blockState = state;
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
        this.blockState = state;
        this.renderType = 1;
        this.lightValue = (byte)ctx.lightEmission();
        this.setLocalPos(ctx);
    }

    @Override
    public void finishRenderingBlock() {
        this.blockId = -1;
        this.blockState = AIR;
        this.renderType = -1;
        this.localPosX = 0;
        this.localPosY = 0;
        this.localPosZ = 0;
        this.lightValue = 0;
        this.ignoreMidBlock = false;
    }

	@Override
	public long writePrimitive(long ptr, Material material, Vertex[] vertices, int chunkId) {
		float uSum = 0.0f;
		float vSum = 0.0f;

		for (Vertex vertex : vertices) {
			uSum += vertex.u;
			vSum += vertex.v;
		}

		float midU = uSum / vertices.length;
		float midV = vSum / vertices.length;

		short blockId = this.blockId;

		//? if <26.1 {
		if (blockId == -1 && fallbackMaterials != null) {
			// Try to fall back to the "canonical" block ID for the texture, this can often greatly improve
			// the visuals of facade-style blocks
			TextureAtlasSprite sprite = ((TextureAtlasExtended)blocksAtlas).celeritas$findFromUV(midU, midV);
			if (sprite != null) {
				blockId = (short) fallbackMaterials.getFallbackId(sprite, this.blockState);
			}
		}
		//?}

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
