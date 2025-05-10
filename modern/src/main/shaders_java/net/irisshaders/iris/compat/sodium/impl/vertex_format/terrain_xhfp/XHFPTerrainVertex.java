package net.irisshaders.iris.compat.sodium.impl.vertex_format.terrain_xhfp;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import net.irisshaders.iris.shaderpack.materialmap.FallbackTextureMaterials;
import net.irisshaders.iris.shaderpack.materialmap.ModernWorldRenderingSettings;
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
import net.irisshaders.iris.vertices.ExtendedDataHelper;
import net.irisshaders.iris.vertices.NormI8;
import net.irisshaders.iris.vertices.NormalHelper;
import org.embeddedt.embeddium.impl.render.texture.TextureAtlasExtended;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

import java.util.Objects;

import static net.irisshaders.iris.compat.sodium.impl.vertex_format.terrain_xhfp.XHFPModelVertexType.STRIDE;

public class XHFPTerrainVertex implements ChunkVertexEncoder, ContextAwareChunkVertexEncoder {

	private final QuadViewTerrain.QuadViewTerrainUnsafe quad = new QuadViewTerrain.QuadViewTerrainUnsafe();
	private final Vector3f normal = new Vector3f();

	private int vertexCount;
	private float uSum;
	private float vSum;

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private int localPosX;
    private int localPosY;
    private int localPosZ;

    private short blockId;
    private BlockState blockState;
    private short renderType;
    private boolean ignoreMidBlock;
    private byte lightValue;

    private final Object2IntMap<BlockState> blockStateIds = Objects.requireNonNullElse(ModernWorldRenderingSettings.INSTANCE.getBlockStateIds(), Object2IntMaps.emptyMap());

    @SuppressWarnings("deprecation")
    private final TextureAtlas blocksAtlas = (TextureAtlas)Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS);

    private final FallbackTextureMaterials fallbackMaterials = ModernWorldRenderingSettings.INSTANCE.getFallbackTextureMaterialMapping();

    private final ChunkVertexEncoder baseEncoder = XHFPModelVertexType.BASE_VERTEX_TYPE.createEncoder();

    private static final int MID_TEX_OFFSET = XHFPModelVertexType.VERTEX_FORMAT.getAttribute("mc_midTexCoord").getPointer();
    private static final int MC_ENTITY_OFFSET = XHFPModelVertexType.VERTEX_FORMAT.getAttribute("mc_Entity").getPointer();
    private static final int MID_BLOCK_OFFSET = XHFPModelVertexType.VERTEX_FORMAT.getAttribute("at_midBlock").getPointer();
    private static final int NORMAL_OFFSET = XHFPModelVertexType.VERTEX_FORMAT.getAttribute("iris_Normal").getPointer();
    private static final int TANGENT_OFFSET = XHFPModelVertexType.VERTEX_FORMAT.getAttribute("at_tangent").getPointer();

    private void setLocalPos(BlockRenderContext ctx) {
        var pos = ctx.pos();
        this.localPosX = pos.getX();
        this.localPosY = pos.getY();
        this.localPosZ = pos.getZ();
    }

    @Override
    public void prepareToRenderBlockFace(BlockRenderContext ctx, @Nullable Direction side) {
        var state = ctx.state();
        this.blockId = (short) this.blockStateIds.getOrDefault(state, -1);
        //? if forgelike && >=1.19 {
        if (this.blockId == -1 && side != null) {
            // Attempt fallback using getAppearance. This is the plan B for block IDs; plan C will use the quad texture
            // to deduce a material.
            BlockState appearanceState = state.getAppearance(ctx.localSlice(), ctx.pos(), side, null, null);
            if (appearanceState != state && !appearanceState.isAir()) {
                this.blockId = (short)this.blockStateIds.getOrDefault(appearanceState, -1);
                state = appearanceState;
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
	public long write(long ptr,
					  Material material, Vertex vertex, int chunkId) {
		uSum += vertex.u;
		vSum += vertex.v;
		vertexCount++;

        this.baseEncoder.write(ptr, material, vertex, chunkId);

        MemoryUtil.memPutShort(ptr + MC_ENTITY_OFFSET + 2, renderType);
        MemoryUtil.memPutInt(ptr + MID_BLOCK_OFFSET, ignoreMidBlock ? 0 : ExtendedDataHelper.computeMidBlock(vertex.x, vertex.y, vertex.z, localPosX, localPosY, localPosZ));
        MemoryUtil.memPutByte(ptr + MID_BLOCK_OFFSET + 3, lightValue);

		if (vertexCount == 4) {
			vertexCount = 0;

			uSum *= 0.25f;
			vSum *= 0.25f;

			int midUV = XHFPModelVertexType.encodeTexture(uSum, vSum);

			MemoryUtil.memPutInt(ptr + MID_TEX_OFFSET, midUV);
			MemoryUtil.memPutInt(ptr + MID_TEX_OFFSET - STRIDE, midUV);
			MemoryUtil.memPutInt(ptr + MID_TEX_OFFSET - STRIDE * 2, midUV);
			MemoryUtil.memPutInt(ptr + MID_TEX_OFFSET - STRIDE * 3, midUV);

            short blockId = this.blockId;

            if (blockId == -1 && fallbackMaterials != null) {
                // Try to fall back to the "canonical" block ID for the texture, this can often greatly improve
                // the visuals of facade-style blocks
                TextureAtlasSprite sprite = ((TextureAtlasExtended)blocksAtlas).celeritas$findFromUV(uSum, vSum);
                if (sprite != null) {
                    blockId = (short) fallbackMaterials.getFallbackId(sprite, this.blockState);
                }
            }

            MemoryUtil.memPutShort(ptr + MC_ENTITY_OFFSET, blockId);
            MemoryUtil.memPutShort(ptr + MC_ENTITY_OFFSET - STRIDE, blockId);
            MemoryUtil.memPutShort(ptr + MC_ENTITY_OFFSET - STRIDE * 2, blockId);
            MemoryUtil.memPutShort(ptr + MC_ENTITY_OFFSET - STRIDE * 3, blockId);

			uSum = 0;
			vSum = 0;

			// normal computation
			// Implementation based on the algorithm found here:
			// https://github.com/IrisShaders/ShaderDoc/blob/master/vertex-format-extensions.md#surface-normal-vector

			quad.setup(ptr, STRIDE);
			NormalHelper.computeFaceNormal(normal, quad);
			int packedNormal = NormI8.pack(normal);


			MemoryUtil.memPutInt(ptr + NORMAL_OFFSET, packedNormal);
			MemoryUtil.memPutInt(ptr + NORMAL_OFFSET - STRIDE, packedNormal);
			MemoryUtil.memPutInt(ptr + NORMAL_OFFSET - STRIDE * 2, packedNormal);
			MemoryUtil.memPutInt(ptr + NORMAL_OFFSET - STRIDE * 3, packedNormal);

			int tangent = NormalHelper.computeTangent(normal.x, normal.y, normal.z, quad);

			MemoryUtil.memPutInt(ptr + TANGENT_OFFSET, tangent);
			MemoryUtil.memPutInt(ptr + TANGENT_OFFSET - STRIDE, tangent);
			MemoryUtil.memPutInt(ptr + TANGENT_OFFSET - STRIDE * 2, tangent);
			MemoryUtil.memPutInt(ptr + TANGENT_OFFSET - STRIDE * 3, tangent);
		}

		return ptr + STRIDE;
	}
}
