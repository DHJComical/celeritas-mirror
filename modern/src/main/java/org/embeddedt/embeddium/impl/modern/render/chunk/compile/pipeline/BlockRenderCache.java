package org.embeddedt.embeddium.impl.modern.render.chunk.compile.pipeline;

import lombok.Getter;
import net.irisshaders.iris.shaderpack.materialmap.ModernWorldRenderingSettings;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.embeddedt.embeddium.impl.model.color.ColorProviderRegistry;
import org.embeddedt.embeddium.impl.model.light.LightPipelineProvider;
import org.embeddedt.embeddium.impl.model.light.data.ArrayLightDataCache;
//? if forgelike && <1.19 {
/*import org.embeddedt.embeddium.impl.render.EmbeddiumRenderLayerCache;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
*///?}
import org.embeddedt.embeddium.impl.world.WorldSlice;
import org.embeddedt.embeddium.impl.world.cloned.ChunkRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockModelShaper;

/**
 * Holds important caches and working data structures for a single chunk meshing thread. All objects within
 * this class do not need to be thread-safe or lightweight to construct, as a separate instance is allocated per thread
 * and reused for the lifetime of that thread.
 */
public class BlockRenderCache {
    private final ArrayLightDataCache lightDataCache;

    private final BlockRenderer blockRenderer;
    private final FluidRenderer fluidRenderer;
    private final LightPipelineProvider lightPipelineProvider;
    @Getter
    private final SpecialBlockRenderer specialBlockRenderer;

    private final BlockModelShaper blockModels;
    private final WorldSlice worldSlice;

    //? if forgelike && <1.19 {
    /*@Getter
    private final EmbeddiumRenderLayerCache<BlockState, RenderType> blockRenderLayerCache = new EmbeddiumRenderLayerCache<>(RenderType.chunkBufferLayers(), ItemBlockRenderTypes::canRenderInLayer);
    @Getter
    private final EmbeddiumRenderLayerCache<FluidState, RenderType> fluidRenderLayerCache = new EmbeddiumRenderLayerCache<>(RenderType.chunkBufferLayers(), ItemBlockRenderTypes::canRenderInLayer);
    *///?}

    public BlockRenderCache(Minecraft client, ClientLevel world) {
        this.worldSlice = new WorldSlice(world);
        this.lightDataCache = new ArrayLightDataCache(this.worldSlice);

        LightPipelineProvider lightPipelineProvider = new LightPipelineProvider(this.lightDataCache, !WorldRenderingSettings.INSTANCE.shouldDisableDirectionalShading());

        var colorRegistry = new ColorProviderRegistry(client.getBlockColors());

        this.blockRenderer = new BlockRenderer(colorRegistry, lightPipelineProvider, ModernWorldRenderingSettings.INSTANCE.getBlockTypeIds());
        this.fluidRenderer = new FluidRenderer(colorRegistry, lightPipelineProvider);
        this.lightPipelineProvider = lightPipelineProvider;
        this.specialBlockRenderer = new SpecialBlockRenderer();

        this.blockModels = client.getModelManager().getBlockModelShaper();
    }

    public BlockModelShaper getBlockModels() {
        return this.blockModels;
    }

    public BlockRenderer getBlockRenderer() {
        return this.blockRenderer;
    }

    public FluidRenderer getFluidRenderer() {
        return this.fluidRenderer;
    }

    /**
     * Initialize the render cache for a new chunk.
     */
    public void init(ChunkRenderContext context) {
        this.lightDataCache.reset(context.getOrigin());
        this.lightPipelineProvider.reset();
        this.worldSlice.copyData(context);
    }

    public WorldSlice getWorldSlice() {
        return this.worldSlice;
    }

    public void cleanup() {
        this.worldSlice.reset();
    }
}
