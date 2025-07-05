package org.embeddedt.embeddium.impl.world.cloned;

import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMaps;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import lombok.Getter;
import net.minecraft.Util;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.chunk.*;
import org.embeddedt.embeddium.impl.model.ModelDataSnapshotter;
import org.embeddedt.embeddium.impl.util.PositionUtil;
import org.embeddedt.embeddium.impl.util.WorldUtil;
import org.embeddedt.embeddium.impl.world.ChunkBiomeContainerExtended;
import org.embeddedt.embeddium.impl.world.ReadableContainerExtended;
import org.embeddedt.embeddium.impl.world.WorldSlice;
//? if ffapi {
//? if <1.20
/*import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachmentBlockEntity;*/
//? if >=1.20
import net.fabricmc.fabric.api.blockview.v2.RenderDataBlockEntity;
//?}
import net.minecraft.core.BlockPos;
//? if >=1.18.2
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.DebugLevelSource;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class ClonedChunkSection {
    //? if >=1.18 {
    private static final int DATA_LAYER_COUNT = DataLayer.LAYER_COUNT;
    //?} else {
    /*private static final int DATA_LAYER_COUNT = 16;
    *///?}

    //? if >=1.20 {
    private static final DataLayer DEFAULT_SKY_LIGHT_ARRAY = new DataLayer(15);
    private static final DataLayer DEFAULT_BLOCK_LIGHT_ARRAY = new DataLayer(0);
    //?} else {
    /*private static final DataLayer DEFAULT_SKY_LIGHT_ARRAY = Util.make(() -> {
        var layer = new DataLayer();
        for(int y = 0; y < DATA_LAYER_COUNT; y++) {
            for(int z = 0; z < DATA_LAYER_COUNT; z++) {
                for(int x = 0; x < DATA_LAYER_COUNT; x++) {
                    layer.set(x, y, z, 15);
                }
            }
        }
        return layer;
    });
    private static final DataLayer DEFAULT_BLOCK_LIGHT_ARRAY = new DataLayer();
    *///?}

    //? if >=1.18 {
    private static final PalettedContainer<BlockState> DEFAULT_STATE_CONTAINER = new PalettedContainer<>(Block.BLOCK_STATE_REGISTRY, Blocks.AIR.defaultBlockState(), PalettedContainer.Strategy.SECTION_STATES);
    //?} else {
    /*private static final GlobalPalette<BlockState> GLOBAL_STATE_PALETTE = new GlobalPalette(Block.BLOCK_STATE_REGISTRY, Blocks.AIR.defaultBlockState());
    private static final PalettedContainer<BlockState> DEFAULT_STATE_CONTAINER = new PalettedContainer<>(GLOBAL_STATE_PALETTE, Block.BLOCK_STATE_REGISTRY, NbtUtils::readBlockState, NbtUtils::writeBlockState, Blocks.AIR.defaultBlockState());
    *///?}
    private static final boolean HAS_FABRIC_RENDER_DATA;

    private final SectionPos pos;

    private final @Nullable Int2ReferenceMap<BlockEntity> blockEntityMap;
    private final @Nullable Int2ReferenceMap<Object> blockEntityRenderDataMap;
    @Getter
    private final ModelDataSnapshotter.Getter modelDataGetter;

    private final @Nullable DataLayer[] lightDataArrays;

    private final @Nullable PalettedContainer<BlockState> blockData;

    //? if >=1.18.2
    private final @Nullable PalettedContainer<Holder<Biome>> biomeData;

    //? if <1.18 {
    /*@Getter
    private final @Nullable ChunkBiomeContainer biomeData;
    *///?}

    private long lastUsedTimestamp = Long.MAX_VALUE;

    static {
        //? if ffapi {
        boolean hasRenderData;
        try {
            //? if <1.20
            /*hasRenderData = RenderAttachmentBlockEntity.class.isAssignableFrom(BlockEntity.class);*/
            //? if >=1.20
            hasRenderData = RenderDataBlockEntity.class.isAssignableFrom(BlockEntity.class);
        } catch(Throwable e) {
            hasRenderData = false;
        }
        HAS_FABRIC_RENDER_DATA = hasRenderData;
        //?} else {
        /*HAS_FABRIC_RENDER_DATA = false;
        *///?}
    }

    public ClonedChunkSection(Level world, LevelChunk chunk, @Nullable LevelChunkSection section, SectionPos pos) {
        this.pos = pos;

        PalettedContainer<BlockState> blockData = null;
        //? if >=1.18 {
        PalettedContainer<Holder<Biome>> biomeData = null;
        //?} else
        /*ChunkBiomeContainer biomeData = null;*/

        Int2ReferenceMap<BlockEntity> blockEntityMap = null;
        Int2ReferenceMap<Object> blockEntityRenderDataMap = null;

        if (section != null) {
            if (!WorldUtil.isSectionEmpty(section)) {
                if (!WorldUtil.isDebug(world)) {
                    blockData = ReadableContainerExtended.clone(section.getStates());
                } else {
                    blockData = constructDebugWorldContainer(pos);
                }
                blockEntityMap = copyBlockEntities(chunk, pos);

                if (blockEntityMap != null) {
                    blockEntityRenderDataMap = copyBlockEntityRenderData(blockEntityMap);
                }
            }

            //? if >=1.18
            biomeData = ReadableContainerExtended.clone((PalettedContainer<Holder<Biome>>)section.getBiomes());
        }

        //? if <1.18
        /*biomeData = ChunkBiomeContainerExtended.clone(chunk.getBiomes());*/

        this.blockData = blockData;
        this.biomeData = biomeData;

        this.blockEntityMap = blockEntityMap;
        this.blockEntityRenderDataMap = blockEntityRenderDataMap;

        this.lightDataArrays = copyLightData(world, pos);

        this.modelDataGetter = ModelDataSnapshotter.getModelDataForSection(world, pos);
    }

    /**
     * Construct a fake PalettedContainer whose contents match those of the debug world. This is needed to
     * match vanilla's odd approach of short-circuiting getBlockState calls inside its render region class.
     */
    @NotNull
    private static PalettedContainer<BlockState> constructDebugWorldContainer(SectionPos pos) {
        // Fast path for sections which are guaranteed to be empty
        if (pos.getY() != 3 && pos.getY() != 4)
            return DEFAULT_STATE_CONTAINER;

        // We use swapUnsafe in the loops to avoid acquiring/releasing the lock on each iteration
        //? if >=1.18 {
        var container = new PalettedContainer<>(Block.BLOCK_STATE_REGISTRY, Blocks.AIR.defaultBlockState(), PalettedContainer.Strategy.SECTION_STATES);
        //?} else
        /*var container = new PalettedContainer<>(GLOBAL_STATE_PALETTE, Block.BLOCK_STATE_REGISTRY, NbtUtils::readBlockState, NbtUtils::writeBlockState, Blocks.AIR.defaultBlockState());*/

        if (pos.getY() == 3) {
            // Set the blocks at relative Y 12 (world Y 60) to barriers
            BlockState barrier = Blocks.BARRIER.defaultBlockState();
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    container.getAndSetUnchecked(x, 12, z, barrier);
                }
            }
        } else if (pos.getY() == 4) {
            // Set the blocks at relative Y 6 (world Y 70) to the appropriate state from the generator
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    container.getAndSetUnchecked(x, 6, z, DebugLevelSource.getBlockStateFor(PositionUtil.sectionToBlockCoord(pos.getX(), x), PositionUtil.sectionToBlockCoord(pos.getZ(), z)));
                }
            }
        }
        return container;
    }

    @NotNull
    private static DataLayer[] copyLightData(Level world, SectionPos pos) {
        var arrays = new DataLayer[2];
        arrays[LightLayer.BLOCK.ordinal()] = copyLightArray(world, LightLayer.BLOCK, pos);

        // Dimensions without sky-light should not have a default-initialized array
        if (WorldUtil.hasSkyLight(world)) {
            arrays[LightLayer.SKY.ordinal()] = copyLightArray(world, LightLayer.SKY, pos);
        }

        return arrays;
    }

    /**
     * Copies the light data array for the given light type for this chunk, or returns a default-initialized value if
     * the light array is not loaded.
     */
    @NotNull
    private static DataLayer copyLightArray(Level world, LightLayer type, SectionPos pos) {
        var array = world.getLightEngine()
                .getLayerListener(type)
                .getDataLayerData(pos);

        if (array == null) {
            array = switch (type) {
                case SKY -> DEFAULT_SKY_LIGHT_ARRAY;
                case BLOCK -> DEFAULT_BLOCK_LIGHT_ARRAY;
            };
        }

        return array;
    }

    @Nullable
    private static Int2ReferenceMap<BlockEntity> copyBlockEntities(LevelChunk chunk, SectionPos chunkCoord) {
        var chunkBlockEntityMap = chunk.getBlockEntities();

        if (chunkBlockEntityMap.isEmpty()) {
            return null;
        }

        BoundingBox box = new BoundingBox(chunkCoord.minBlockX(), chunkCoord.minBlockY(), chunkCoord.minBlockZ(),
                chunkCoord.maxBlockX(), chunkCoord.maxBlockY(), chunkCoord.maxBlockZ());

        Int2ReferenceOpenHashMap<BlockEntity> blockEntities = null;

        // Copy the block entities from the chunk into our cloned section
        for (Map.Entry<BlockPos, BlockEntity> entry : chunkBlockEntityMap.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockEntity entity = entry.getValue();

            if (box.isInside(pos)) {
                if (blockEntities == null) {
                    blockEntities = new Int2ReferenceOpenHashMap<>();
                }

                blockEntities.put(WorldSlice.getLocalBlockIndex(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15), entity);
            }
        }

        if (blockEntities != null) {
            blockEntities.trim();
        }

        return blockEntities;
    }

    @Nullable
    private static Int2ReferenceMap<Object> copyBlockEntityRenderData(Int2ReferenceMap<BlockEntity> blockEntities) {
        // Immediately exit if block entities do not have Fabric render data.
        if(!HAS_FABRIC_RENDER_DATA) {
            return null;
        }

        Int2ReferenceOpenHashMap<Object> blockEntityRenderDataMap = null;

        // Retrieve any render data after we have copied all block entities, as this will call into the code of
        // other mods. This could potentially result in the chunk being modified, which would cause problems if we
        // were iterating over any data in that chunk.
        // See https://github.com/CaffeineMC/sodium-fabric/issues/942 for more info.
        for (var entry : Int2ReferenceMaps.fastIterable(blockEntities)) {
            //? if ffapi {
            //? if >=1.20
            Object data = ((RenderDataBlockEntity)entry.getValue()).getRenderData();
            //? if <1.20
            /*Object data = ((RenderAttachmentBlockEntity)entry.getValue()).getRenderAttachmentData();*/
            //?} else {
            /*Object data = null;
            *///?}

            if (data != null) {
                if (blockEntityRenderDataMap == null) {
                    blockEntityRenderDataMap = new Int2ReferenceOpenHashMap<>();
                }

                blockEntityRenderDataMap.put(entry.getIntKey(), data);
            }
        }

        if (blockEntityRenderDataMap != null) {
            blockEntityRenderDataMap.trim();
        }

        return blockEntityRenderDataMap;
    }

    public SectionPos getPosition() {
        return this.pos;
    }

    public @Nullable PalettedContainer<BlockState> getBlockData() {
        return this.blockData;
    }

    //? if >=1.18.2 {
    public @Nullable PalettedContainer<Holder<Biome>> getBiomeData() {
        return this.biomeData;
    }
    //?}

    public @Nullable Int2ReferenceMap<BlockEntity> getBlockEntityMap() {
        return this.blockEntityMap;
    }

    public @Nullable Int2ReferenceMap<Object> getBlockEntityRenderDataMap() {
        return this.blockEntityRenderDataMap;
    }

    public @Nullable DataLayer getLightArray(LightLayer lightType) {
        return this.lightDataArrays[lightType.ordinal()];
    }

    public long getLastUsedTimestamp() {
        return this.lastUsedTimestamp;
    }

    public void setLastUsedTimestamp(long timestamp) {
        this.lastUsedTimestamp = timestamp;
    }

    public BlockState getBlockState(int x, int y, int z) {
        if (this.blockData != null) {
            return this.blockData.get(x, y, z);
        } else {
            return Blocks.AIR.defaultBlockState();
        }
    }
}
