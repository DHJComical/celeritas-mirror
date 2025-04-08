package net.irisshaders.iris.shaderpack.materialmap;

import it.unimi.dsi.fastutil.objects.Object2IntFunction;
import org.jetbrains.annotations.Nullable;

import static org.embeddedt.embeddium.compat.mc.MinecraftVersionShimService.MINECRAFT_SHIM;

public class WorldRenderingSettings {
	public static final WorldRenderingSettings INSTANCE = new WorldRenderingSettings();

	private boolean reloadRequired;

	private Object2IntFunction<NamespacedId> entityIds;
	private Object2IntFunction<NamespacedId> itemIds;
	private float ambientOcclusionLevel;
	private boolean disableDirectionalShading;
	private boolean hasVillagerConversionId;
	private boolean useSeparateAo;
	private boolean useExtendedVertexFormat;
	private boolean separateEntityDraws;
	private boolean voxelizeLightBlocks;

	public WorldRenderingSettings() {
		reloadRequired = false;
		ambientOcclusionLevel = 1.0F;
		disableDirectionalShading = false;
		useSeparateAo = false;
		useExtendedVertexFormat = false;
		separateEntityDraws = false;
		voxelizeLightBlocks = false;
		hasVillagerConversionId = false;
	}

	public boolean isReloadRequired() {
		return reloadRequired;
	}

	public void clearReloadRequired() {
		reloadRequired = false;
	}

    public void reloadRendererIfRequired() {
        if (isReloadRequired()) {
            MINECRAFT_SHIM.markRendererReloadRequired();
            clearReloadRequired();
        }
    }


	@Nullable
	public Object2IntFunction<NamespacedId> getEntityIds() {
		return entityIds;
	}

	public void setEntityIds(Object2IntFunction<NamespacedId> entityIds) {
		// note: no reload needed, entities are rebuilt every frame.
		this.entityIds = entityIds;
		this.hasVillagerConversionId = entityIds.containsKey(new NamespacedId("minecraft", "zombie_villager_converting"));
	}

	@Nullable
	public Object2IntFunction<NamespacedId> getItemIds() {
		return itemIds;
	}

	public void setItemIds(Object2IntFunction<NamespacedId> itemIds) {
		// note: no reload needed, entities are rebuilt every frame.
		this.itemIds = itemIds;
	}

	public float getAmbientOcclusionLevel() {
		return ambientOcclusionLevel;
	}

	public void setAmbientOcclusionLevel(float ambientOcclusionLevel) {
		if (ambientOcclusionLevel == this.ambientOcclusionLevel) {
			return;
		}

		this.reloadRequired = true;
		this.ambientOcclusionLevel = ambientOcclusionLevel;
	}

	public boolean shouldDisableDirectionalShading() {
		return disableDirectionalShading;
	}

	public void setDisableDirectionalShading(boolean disableDirectionalShading) {
		if (disableDirectionalShading == this.disableDirectionalShading) {
			return;
		}

		this.reloadRequired = true;
		this.disableDirectionalShading = disableDirectionalShading;
	}

	public boolean shouldUseSeparateAo() {
		return useSeparateAo;
	}

	public void setUseSeparateAo(boolean useSeparateAo) {
		if (useSeparateAo == this.useSeparateAo) {
			return;
		}

		this.reloadRequired = true;
		this.useSeparateAo = useSeparateAo;
	}

	public boolean shouldUseExtendedVertexFormat() {
		return useExtendedVertexFormat;
	}

	public void setUseExtendedVertexFormat(boolean useExtendedVertexFormat) {
		if (useExtendedVertexFormat == this.useExtendedVertexFormat) {
			return;
		}

		this.reloadRequired = true;
		this.useExtendedVertexFormat = useExtendedVertexFormat;
	}

	public boolean shouldVoxelizeLightBlocks() {
		return voxelizeLightBlocks;
	}

	public void setVoxelizeLightBlocks(boolean voxelizeLightBlocks) {
		if (voxelizeLightBlocks == this.voxelizeLightBlocks) {
			return;
		}

		this.reloadRequired = true;
		this.voxelizeLightBlocks = voxelizeLightBlocks;
	}

	public boolean shouldSeparateEntityDraws() {
		return separateEntityDraws;
	}

	public void setSeparateEntityDraws(boolean separateEntityDraws) {
		this.separateEntityDraws = separateEntityDraws;
	}

	public boolean hasVillagerConversionId() {
		return hasVillagerConversionId;
	}

    public void setReloadRequired() {
        reloadRequired = true;
    }
}
