package net.irisshaders.iris.compat.sodium.impl.block_context;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class BlockContextHolder {
	private final Object2IntMap<BlockState> blockStateIds;
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

	public int localPosX;
	public int localPosY;
	public int localPosZ;

	public short blockId;
    public BlockState blockState;
	public short renderType;
	public boolean ignoreMidBlock;
	public byte lightValue;

	public BlockContextHolder() {
		this.blockStateIds = Object2IntMaps.emptyMap();
		this.blockId = -1;
		this.renderType = -1;
		this.lightValue = 0;
        this.blockState = AIR;
	}

	public BlockContextHolder(Object2IntMap<BlockState> idMap) {
		this.blockStateIds = idMap;
		this.blockId = -1;
		this.renderType = -1;
        this.blockState = AIR;
	}

	public void setLocalPos(int localPosX, int localPosY, int localPosZ) {
		this.localPosX = localPosX;
		this.localPosY = localPosY;
		this.localPosZ = localPosZ;
	}

	public void set(BlockState state, short renderType, byte lightValue) {
		this.blockId = (short) this.blockStateIds.getOrDefault(state, -1);
        this.blockState = state;
		this.renderType = renderType;
		this.lightValue = lightValue;
	}

	public void reset() {
		this.blockId = -1;
        this.blockState = AIR;
		this.renderType = -1;
		this.localPosX = 0;
		this.localPosY = 0;
		this.localPosZ = 0;
		this.lightValue = 0;
	}
}
