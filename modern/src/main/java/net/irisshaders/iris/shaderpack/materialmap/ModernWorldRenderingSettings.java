package net.irisshaders.iris.shaderpack.materialmap;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;


import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

public class ModernWorldRenderingSettings {
    public static final ModernWorldRenderingSettings INSTANCE = new ModernWorldRenderingSettings();

    private Object2IntMap<BlockState> blockStateIds;
    private FallbackTextureMaterials fallbackTextureMaterialMapping;
    private Map<Block, RenderType> blockTypeIds;

    public ModernWorldRenderingSettings() {
        this.blockStateIds = null;
        this.fallbackTextureMaterialMapping = null;
        this.blockTypeIds = null;
    }

    @Nullable
    public Object2IntMap<BlockState> getBlockStateIds() {
        return blockStateIds;
    }

    public void setBlockStateIds(Object2IntMap<BlockState> blockStateIds) {
        if (Objects.equals(this.blockStateIds, blockStateIds)) {
            return;
        }
        WorldRenderingSettings.INSTANCE.setReloadRequired();
        this.blockStateIds = blockStateIds;
    }

    @Nullable
    public FallbackTextureMaterials getFallbackTextureMaterialMapping() {
        return fallbackTextureMaterialMapping;
    }

    public void setFallbackTextureMaterialMapping(FallbackTextureMaterials fallbackTextureMaterialMapping) {
        if (Objects.equals(this.fallbackTextureMaterialMapping, fallbackTextureMaterialMapping)) {
            return;
        }

        WorldRenderingSettings.INSTANCE.setReloadRequired();
        this.fallbackTextureMaterialMapping = fallbackTextureMaterialMapping;
    }


    @Nullable
    public Map<Block, RenderType> getBlockTypeIds() {
        return blockTypeIds;
    }

    public void setBlockTypeIds(Map<Block, RenderType> blockTypeIds) {
        if (Objects.equals(this.blockTypeIds, blockTypeIds)) {
            return;
        }

        WorldRenderingSettings.INSTANCE.setReloadRequired();
        this.blockTypeIds = blockTypeIds;
    }
}
