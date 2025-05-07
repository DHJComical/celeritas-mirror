package org.taumc.celeritas.impl.world.cloned;

import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import lombok.Getter;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import org.embeddedt.embeddium.impl.util.position.SectionPos;
import org.taumc.celeritas.impl.compat.fluidlogged.FluidStateStorage;
import org.taumc.celeritas.impl.compat.fluidlogged.FluidloggedCompat;

import java.util.Map;

public class ClonedChunkSection {
    private static final ExtendedBlockStorage EMPTY_SECTION = new ExtendedBlockStorage(0, false);

    private final Short2ObjectMap<TileEntity> blockEntities;
    private final World world;

    private final ExtendedBlockStorage data;
    @Getter
    private final FluidStateStorage fluidData;

    private final Biome[] biomeData;

    private byte[][] lightData;

    private long lastUsedTimestamp = Long.MAX_VALUE;

    private final SectionPos sectionPos;

    ClonedChunkSection(World world, int x, int y, int z) {
        this.world = world;
        this.blockEntities = new Short2ObjectOpenHashMap<>();
        this.sectionPos = new SectionPos(x, y, z);

        Chunk chunk = world.getChunk(x, z);

        if (chunk == null) {
            throw new RuntimeException("Couldn't retrieve chunk at %d, %d".formatted(x, z));
        }

        ExtendedBlockStorage section = getChunkSection(chunk, y);

        if (section == Chunk.NULL_BLOCK_STORAGE/*ChunkSection.isEmpty(section)*/) {
            section = EMPTY_SECTION;
        }

        this.data = section;
        if (FluidloggedCompat.IS_LOADED) {
            this.fluidData = new FluidStateStorage(chunk, y << 4);
        } else {
            this.fluidData = null;
        }
        this.biomeData = new Biome[chunk.getBiomeArray().length];

        if (!chunk.getTileEntityMap().isEmpty()) {
            StructureBoundingBox box = new StructureBoundingBox(this.sectionPos.minX(), this.sectionPos.minY(), this.sectionPos.minZ(),
                    this.sectionPos.maxX(), this.sectionPos.maxY(), this.sectionPos.maxZ());

            for (Map.Entry<BlockPos, TileEntity> entry : chunk.getTileEntityMap().entrySet()) {
                BlockPos entityPos = entry.getKey();

                if (box.isVecInside(entityPos)) {
                    this.blockEntities.put(packLocal(entityPos.getX() & 15, entityPos.getY() & 15, entityPos.getZ() & 15), entry.getValue());
                }
            }
        }

        populateBiomeData(x, z, world);

    }

    private void populateBiomeData(int chunkX, int chunkZ, World world) {
        BlockPos.MutableBlockPos biomePos = new BlockPos.MutableBlockPos();

        chunkX *= 16;
        chunkZ *= 16;
        // Fill biome data
        for(int z = 0; z < 16; z++) {
            for(int x = 0; x < 16; x++) {
                biomePos.setPos(chunkX + x, 100, chunkZ + z);
                this.biomeData[((z & 15) << 4) | (x & 15)] = world.getBiome(biomePos);
            }
        }
    }

    public IBlockState getBlockState(int x, int y, int z) {
        return data.get(x, y, z);
    }

    public Biome getBiomeForNoiseGen(int x, int z) {
        return this.biomeData[x | z << 4];
    }

    public Biome[] getBiomeData() {
        return this.biomeData;
    }

    public TileEntity getBlockEntity(int x, int y, int z) {
        return this.blockEntities.get(packLocal(x, y, z));
    }

    public SectionPos getPosition() {
        return this.sectionPos;
    }

    public int getLightLevel(int x, int y, int z, EnumSkyBlock type) {
        NibbleArray lightArray = type == EnumSkyBlock.BLOCK ? this.data.getBlockLight() : this.data.getSkyLight();
        return lightArray != null ? lightArray.get(x, y, z) : type.defaultLightValue;
    }

    private static ExtendedBlockStorage getChunkSection(Chunk chunk, int y) {
        ExtendedBlockStorage section = null;

        var storageArray = chunk.getBlockStorageArray();

        if (y >= 0 && y < storageArray.length) {
            section = storageArray[y];
        }

        return section;
    }

    public long getLastUsedTimestamp() {
        return this.lastUsedTimestamp;
    }

    public void setLastUsedTimestamp(long timestamp) {
        this.lastUsedTimestamp = timestamp;
    }

    /**
     * @param x The local x-coordinate
     * @param y The local y-coordinate
     * @param z The local z-coordinate
     * @return An index which can be used to key entities or blocks within a chunk
     */
    private static short packLocal(int x, int y, int z) {
        return (short) (x << 8 | z << 4 | y);
    }
}
