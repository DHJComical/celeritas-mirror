package net.irisshaders.iris.layer;

import it.unimi.dsi.fastutil.objects.Object2IntFunction;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.ZombieVillagerRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SolidBucketItem;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The entity, block entity and item IDs that the extended vertex formats pass to shader packs. Everything is drawn some
 * time after it's submitted, so each submit keeps the IDs that were current when it was submitted, and restores them
 * when it's drawn.
 */
public final class RenderContextIds {
	public static final NamespacedId NAME_TAG = new NamespacedId("minecraft", "name_tag");
	public static final NamespacedId ENTITY_FLAME = new NamespacedId("minecraft", "entity_flame");
	public static final NamespacedId ENTITY_SHADOW = new NamespacedId("minecraft", "entity_shadow");
	private static final NamespacedId CONVERTING_VILLAGER = new NamespacedId("minecraft", "zombie_villager_converting");
	private static final NamespacedId CURRENT_PLAYER = new NamespacedId("minecraft", "current_player");
	private static final Reference2ObjectMap<EntityType<?>, NamespacedId> ENTITY_TYPE_IDS = new Reference2ObjectOpenHashMap<>();

	private RenderContextIds() {
	}

	/**
	 * A submit that keeps the IDs current when it was created.
	 */
	public interface Holder {
		void iris$restoreIds();
	}

	public static void setEntity(EntityRenderState state) {
		Object2IntFunction<NamespacedId> entityIds = WorldRenderingSettings.INSTANCE.getEntityIds();

		if (entityIds == null) {
			return;
		}

		NamespacedId id;

		if (state instanceof ZombieVillagerRenderState zombie && zombie.isConverting && WorldRenderingSettings.INSTANCE.hasVillagerConversionId()) {
			id = CONVERTING_VILLAGER;
		} else if (state instanceof AvatarRenderState avatar && entityIds.containsKey(CURRENT_PLAYER) && isCameraEntity(avatar.id)) {
			id = CURRENT_PLAYER;
		} else {
			id = ENTITY_TYPE_IDS.computeIfAbsent(state.entityType, type -> {
				Identifier key = BuiltInRegistries.ENTITY_TYPE.getKey((EntityType<?>) type);
				return new NamespacedId(key.getNamespace(), key.getPath());
			});
		}

		CapturedRenderingState.INSTANCE.setCurrentEntity(entityIds.applyAsInt(id));
	}

	private static boolean isCameraEntity(int entityId) {
		var camera = Minecraft.getInstance().getCameraEntity();
		return camera != null && camera.getId() == entityId;
	}

	/**
	 * Sets an entity ID that isn't an entity type's, such as for name tags.
	 */
	public static void setEntity(NamespacedId id) {
		Object2IntFunction<NamespacedId> entityIds = WorldRenderingSettings.INSTANCE.getEntityIds();

		if (entityIds != null) {
			CapturedRenderingState.INSTANCE.setCurrentEntity(entityIds.applyAsInt(id));
		}
	}

	public static void setBlockEntity(BlockState state) {
		Object2IntMap<BlockState> blockStateIds = WorldRenderingSettings.INSTANCE.getBlockStateIds();

		if (blockStateIds != null) {
			CapturedRenderingState.INSTANCE.setCurrentBlockEntity(blockStateIds.getOrDefault(state, -1));
		}
	}

	/**
	 * Sets the ID of an item. Block items take their block's ID, and mark the block entity ID with 1.
	 */
	public static void setItem(@Nullable Item item, @Nullable Identifier modelId) {
		Object2IntFunction<NamespacedId> itemIds = WorldRenderingSettings.INSTANCE.getItemIds();

		if (itemIds == null || item == null) {
			return;
		}

		if (item instanceof BlockItem blockItem && !(item instanceof SolidBucketItem)) {
			Object2IntMap<BlockState> blockStateIds = WorldRenderingSettings.INSTANCE.getBlockStateIds();

			if (blockStateIds != null) {
				CapturedRenderingState.INSTANCE.setCurrentBlockEntity(1);
				CapturedRenderingState.INSTANCE.setCurrentRenderedItem(blockStateIds.getOrDefault(blockItem.getBlock().defaultBlockState(), 0));
			}
		} else {
			Identifier location = modelId != null ? modelId : BuiltInRegistries.ITEM.getKey(item);
			CapturedRenderingState.INSTANCE.setCurrentRenderedItem(itemIds.applyAsInt(new NamespacedId(location.getNamespace(), location.getPath())));
		}
	}

	/**
	 * Sets an item ID that isn't an item's, such as for capes and armor trims.
	 */
	public static void setItem(NamespacedId id) {
		Object2IntFunction<NamespacedId> itemIds = WorldRenderingSettings.INSTANCE.getItemIds();

		if (itemIds != null) {
			CapturedRenderingState.INSTANCE.setCurrentRenderedItem(itemIds.applyAsInt(id));
		}
	}

	public static void reset() {
		CapturedRenderingState.INSTANCE.setCurrentEntity(0);
		CapturedRenderingState.INSTANCE.setCurrentBlockEntity(0);
		CapturedRenderingState.INSTANCE.setCurrentRenderedItem(0);
	}
}
