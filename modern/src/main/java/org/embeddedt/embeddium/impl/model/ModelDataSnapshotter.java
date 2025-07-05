package org.embeddedt.embeddium.impl.model;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
//? if forge && >=1.19 {
import net.minecraftforge.client.model.data.ModelData;
//?} else if forge && <1.19 {
/*import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.ModelDataManager;
*///?}
//? if neoforge {
/*import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelDataManager;
*///?}
import java.util.Map;

public class ModelDataSnapshotter {
    public interface Getter {
        //? if forgelike && >=1.19 {
        Getter EMPTY = pos -> ModelData.EMPTY;
        ModelData getModelData(BlockPos pos);
        //?} else if forge && <1.19 {
        /*Getter EMPTY = pos -> EmptyModelData.INSTANCE;
        IModelData getModelData(BlockPos pos);
        *///?} else {
        /*Getter EMPTY = new Getter() {};
        *///?}
    }

    /**
     * Retrieve all needed model data for the given subchunk.
     * @param world the client world to retrieve data for
     * @param origin the origin of the subchunk
     * @return a map of all model data contained within this subchunk
     */
    public static Getter getModelDataForSection(Level world, SectionPos origin) {
        //? if forge {

        //? if >=1.19
        Map<BlockPos, ?> forgeMap = world.getModelDataManager().getAt(origin.chunk());
        //? if <1.19
        /*Map<BlockPos, ?> forgeMap = ModelDataManager.getModelData(world, origin.chunk());*/

        // Fast path if there is no model data in this chunk
        if(forgeMap.isEmpty())
            return Getter.EMPTY;

        Long2ObjectOpenHashMap<Object> ourMap = new Long2ObjectOpenHashMap<>();
        //? if >=1.19
        ourMap.defaultReturnValue(ModelData.EMPTY);
        //? if <1.19
        /*ourMap.defaultReturnValue(EmptyModelData.INSTANCE);*/

        BoundingBox volume = new BoundingBox(origin.minBlockX(), origin.minBlockY(), origin.minBlockZ(), origin.maxBlockX(), origin.maxBlockY(), origin.maxBlockZ());

        for(Map.Entry<BlockPos, ?> dataEntry : forgeMap.entrySet()) {
            Object data = dataEntry.getValue();

            if(data == null || data == ourMap.defaultReturnValue()) {
                // There is no reason to populate the map with empty model data, because our
                // getOrDefault call will return the empty instance by default anyway
                continue;
            }

            BlockPos key = dataEntry.getKey();

            if(volume.isInside(key)) {
                ourMap.put(key.asLong(), data);
            }
        }

        return ourMap.isEmpty() ? Getter.EMPTY :
                //? if >=1.19 {
                pos -> (ModelData)ourMap.get(pos.asLong());
                //?} else
                /*pos -> (IModelData)ourMap.get(pos.asLong());*/
        //?} else if neoforge && >=1.20.6 {
        /*var snapshot = world.getModelDataManager().snapshotSectionRegion(origin.getX(), origin.getY(), origin.getZ(), origin.getX(), origin.getY(), origin.getZ());
        if (snapshot == ModelDataManager.EMPTY_SNAPSHOT) {
            // Avoid an extra level of indirection
            return Getter.EMPTY;
        } else {
            return pos -> snapshot.get(pos.asLong());
        }
        *///?} else if neoforge {
        /*var snapshot = world.getModelDataManager().snapshotSectionRegion(origin.getX(), origin.getY(), origin.getZ(), origin.getX(), origin.getY(), origin.getZ());
        if (snapshot == ModelDataManager.Snapshot.EMPTY) {
            return Getter.EMPTY;
        } else {
            return snapshot::getAtOrEmpty;
        }
        *///?} else {
        /*return Getter.EMPTY;
        *///?}
    }
}
