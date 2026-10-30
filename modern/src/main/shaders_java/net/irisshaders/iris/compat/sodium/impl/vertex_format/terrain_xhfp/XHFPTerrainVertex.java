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
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.impl.VanillaLikeChunkVertex;
//? if <26.1
import org.embeddedt.embeddium.impl.render.texture.TextureAtlasExtended;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import org.taumc.celeritas.shaders.CeleritasShaders;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.irisshaders.iris.compat.sodium.impl.vertex_format.terrain_xhfp.XHFPModelVertexType.HEADER_STRIDE;
import static net.irisshaders.iris.compat.sodium.impl.vertex_format.terrain_xhfp.XHFPModelVertexType.VERTEX_STRIDE;

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

    private static final int MC_ENTITY_OFFSET = XHFPModelVertexType.HEADER_FORMAT.getAttribute("mc_Entity").getPointer();
    private static final int BLOCK_INFO_OFFSET = XHFPModelVertexType.HEADER_FORMAT.getAttribute(XHFPModelVertexType.BLOCK_INFO).getPointer();

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
	public boolean supportsBilinearCorrection() {
		return false;
	}

	@Override
	public long writePrimitive(long ptr, Material material, Vertex[] vertices, int chunkId) {
		long vertexPtr = ptr + HEADER_STRIDE;
		float uSum = 0.0f;
		float vSum = 0.0f;

		for (Vertex vertex : vertices) {
			uSum += vertex.u;
			vSum += vertex.v;

			VanillaLikeChunkVertex.writeVertexWithoutRdhFactor(vertexPtr, material, vertex, chunkId);

			vertexPtr += VERTEX_STRIDE;
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

		MemoryUtil.memPutShort(ptr + MC_ENTITY_OFFSET, blockId);
		MemoryUtil.memPutShort(ptr + MC_ENTITY_OFFSET + 2, renderType);

		// Vertex positions are relative to the section, so only the block's position within it is needed
		MemoryUtil.memPutInt(ptr + BLOCK_INFO_OFFSET, (localPosX & 15)
				| ((localPosY & 15) << 4)
				| ((localPosZ & 15) << 8)
				| (ignoreMidBlock ? 0 : 1 << 12)
				| ((lightValue & 0xFF) << 16));

		return vertexPtr;
	}
}
