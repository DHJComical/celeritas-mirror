package net.irisshaders.iris.compat.sodium.impl.vertex_format.terrain_xhfp;

import it.unimi.dsi.fastutil.objects.Object2IntFunction;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.shaderpack.materialmap.FallbackTextureMaterials;
import net.irisshaders.iris.shaderpack.materialmap.ModelTextureAnalyzer;
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
import net.irisshaders.iris.vertices.ExtendedDataHelper;
import net.irisshaders.iris.vertices.NormI8;
import net.irisshaders.iris.vertices.NormalHelper;
import org.embeddedt.embeddium.impl.render.texture.TextureAtlasExtended;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

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

    private final Object2IntMap<BlockState> blockStateIds = WorldRenderingSettings.INSTANCE.getBlockStateIds();

    private final TextureAtlas blocksAtlas = (TextureAtlas)Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS);
    private final FallbackTextureMaterials fallbackMaterials = WorldRenderingSettings.INSTANCE.getFallbackTextureMaterialMapping();

    private static int encodeDrawParameters(Material material, int sectionIndex) {
        return (sectionIndex & 255) << 8 | (material.bits() & 255) << 0;
    }

    private static int encodeLight(int light) {
        int block = light & 255;
        int sky = light >> 16 & 255;
        return block << 0 | sky << 8;
    }

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
    }

    @Override
	public long write(long ptr,
					  Material material, Vertex vertex, int chunkId) {
		uSum += vertex.u;
		vSum += vertex.v;
		vertexCount++;

        MemoryUtil.memPutFloat(ptr, vertex.x);
        MemoryUtil.memPutFloat(ptr + 4, vertex.y);
        MemoryUtil.memPutFloat(ptr + 8, vertex.z);

        MemoryUtil.memPutInt(ptr + 12, vertex.color);

        MemoryUtil.memPutFloat(ptr + 16, vertex.u);
        MemoryUtil.memPutFloat(ptr + 20, vertex.v);

        MemoryUtil.memPutInt(ptr + 24, encodeDrawParameters(material, chunkId) << 0 | encodeLight(vertex.light) << 16);

        MemoryUtil.memPutShort(ptr + 42, renderType);
        MemoryUtil.memPutInt(ptr + 44, ignoreMidBlock ? 0 : ExtendedDataHelper.computeMidBlock(vertex.x, vertex.y, vertex.z, localPosX, localPosY, localPosZ));
        MemoryUtil.memPutByte(ptr + 47, lightValue);

		if (vertexCount == 4) {
			vertexCount = 0;

			// FIXME
			// The following logic is incorrect because OpenGL denormalizes shorts by dividing by 65535. The atlas is
			// based on power-of-two values and so a normalization factor that is not a power of two causes the values
			// used in the shader to be off by enough to cause visual errors. These are most noticeable on 1.18 with POM
			// on block edges.
			//
			// The only reliable way that this can be fixed is to apply the same shader transformations to midTexCoord
			// as Sodium does to the regular texture coordinates - dividing them by the correct power-of-two value inside
			// of the shader instead of letting OpenGL value normalization do the division. However, this requires
			// fragile patching that is not yet possible.
			//
			// As a temporary solution, the normalized shorts have been replaced with regular floats, but this takes up
			// an extra 4 bytes per vertex.

			// NB: Be careful with the math here! A previous bug was caused by midU going negative as a short, which
			// was sign-extended into midTexCoord, causing midV to have garbage (likely NaN data). If you're touching
			// this code, be aware of that, and don't introduce those kinds of bugs!
			//
			// Also note that OpenGL takes shorts in the range of [0, 65535] and transforms them linearly to [0.0, 1.0],
			// so multiply by 65535, not 65536.
			//
			// TODO: Does this introduce precision issues? Do we need to fall back to floats here? This might break
			// with high resolution texture packs.
//			int midU = (int)(65535.0F * Math.min(uSum * 0.25f, 1.0f)) & 0xFFFF;
//			int midV = (int)(65535.0F * Math.min(vSum * 0.25f, 1.0f)) & 0xFFFF;
//			int midTexCoord = (midV << 16) | midU;

			uSum *= 0.25f;
			vSum *= 0.25f;

			int midUV = XHFPModelVertexType.encodeTexture(uSum, vSum);

			MemoryUtil.memPutInt(ptr + 28, midUV);
			MemoryUtil.memPutInt(ptr + 28 - STRIDE, midUV);
			MemoryUtil.memPutInt(ptr + 28 - STRIDE * 2, midUV);
			MemoryUtil.memPutInt(ptr + 28 - STRIDE * 3, midUV);

            short blockId = this.blockId;

            if (blockId == -1 && fallbackMaterials != null) {
                // Try to fall back to the "canonical" block ID for the texture, this can often greatly improve
                // the visuals of facade-style blocks
                TextureAtlasSprite sprite = ((TextureAtlasExtended)blocksAtlas).celeritas$findFromUV(uSum, vSum);
                if (sprite != null) {
                    blockId = (short) fallbackMaterials.getFallbackId(sprite, this.blockState);
                }
            }

            MemoryUtil.memPutShort(ptr + 40, blockId);
            MemoryUtil.memPutShort(ptr + 40 - STRIDE, blockId);
            MemoryUtil.memPutShort(ptr + 40 - STRIDE * 2, blockId);
            MemoryUtil.memPutShort(ptr + 40 - STRIDE * 3, blockId);

			uSum = 0;
			vSum = 0;

			// normal computation
			// Implementation based on the algorithm found here:
			// https://github.com/IrisShaders/ShaderDoc/blob/master/vertex-format-extensions.md#surface-normal-vector

			quad.setup(ptr, STRIDE);
			NormalHelper.computeFaceNormal(normal, quad);
			int packedNormal = NormI8.pack(normal);


			MemoryUtil.memPutInt(ptr + 36, packedNormal);
			MemoryUtil.memPutInt(ptr + 36 - STRIDE, packedNormal);
			MemoryUtil.memPutInt(ptr + 36 - STRIDE * 2, packedNormal);
			MemoryUtil.memPutInt(ptr + 36 - STRIDE * 3, packedNormal);

			int tangent = NormalHelper.computeTangent(normal.x, normal.y, normal.z, quad);

			MemoryUtil.memPutInt(ptr + 32, tangent);
			MemoryUtil.memPutInt(ptr + 32 - STRIDE, tangent);
			MemoryUtil.memPutInt(ptr + 32 - STRIDE * 2, tangent);
			MemoryUtil.memPutInt(ptr + 32 - STRIDE * 3, tangent);
		}

		return ptr + STRIDE;
	}
}
