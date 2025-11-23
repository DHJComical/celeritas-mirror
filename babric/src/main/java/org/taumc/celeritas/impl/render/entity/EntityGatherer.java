package org.taumc.celeritas.impl.render.entity;

//? if >=1.8 {

/*import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.TypeInstanceMultiMap;
import net.minecraft.world.chunk.WorldChunk;
import org.taumc.celeritas.mixin.core.ClientChunkCacheAccessor;
import org.taumc.celeritas.mixin.core.WorldChunkAccessor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class EntityGatherer {
    private final List<Entity> entityList;
    private final Consumer<Entity> addEntity;

    public EntityGatherer() {
        this.entityList = new ArrayList<>();
        this.addEntity = this.entityList::add;
    }

    public void clear() {
        this.entityList.clear();
    }

    public List<Entity> getLoadedEntityList(ClientWorld world) {
        Consumer<Entity> addEntity = this.addEntity;
        // Iterate directly over chunk entity lists where possible - mods may create multipart entities that are not
        // added to the main loadedEntityList.
        if (world.getChunkSource() instanceof ClientChunkCacheAccessor provider) {
            var loadedChunks = provider.getAllChunks();
            for (WorldChunk chunk : loadedChunks) {
                if (!((WorldChunkAccessor)chunk).getHasEntities()) {
                    continue;
                }
                TypeInstanceMultiMap<Entity>[] entityMaps = chunk.getEntities();
                for (TypeInstanceMultiMap<Entity> map : entityMaps) {
                    map.forEach(addEntity);
                }
            }
        } else {
            // Best we can do is the loaded entity list - this will miss some multipart entities
            world.entities.forEach(addEntity);
        }
        return this.entityList;
    }
}

*///?}