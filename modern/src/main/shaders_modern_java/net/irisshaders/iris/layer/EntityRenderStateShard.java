package net.irisshaders.iris.layer;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.minecraft.client.renderer.RenderStateShard;

public final class EntityRenderStateShard extends RenderStateShard {
    private static final Int2ObjectMap<EntityRenderStateShard> SHARDS = new Int2ObjectOpenHashMap<>();

    private static int previousEntityId;

	private EntityRenderStateShard(int entityId) {
		super("iris:is_entity", () -> {
            previousEntityId = CapturedRenderingState.INSTANCE.getCurrentRenderedEntity();
            CapturedRenderingState.INSTANCE.setCurrentEntity(entityId);
            GbufferPrograms.beginEntities();

        }, () -> {
            GbufferPrograms.endEntities();
            CapturedRenderingState.INSTANCE.setCurrentEntity(previousEntityId);
            previousEntityId = 0;
        });
    }

    public static EntityRenderStateShard of(int id) {
        return SHARDS.computeIfAbsent(id, EntityRenderStateShard::new);
    }
}
