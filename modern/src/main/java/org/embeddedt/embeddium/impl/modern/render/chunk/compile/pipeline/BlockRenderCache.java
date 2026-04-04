package org.embeddedt.embeddium.impl.modern.render.chunk.compile.pipeline;

import lombok.Getter;
//? if shaders
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
//? if <26.1 {
import net.minecraft.client.renderer.block.BlockModelShaper;
//?} else
/*import net.minecraft.client.renderer.block.BlockStateModelSet;*/
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.embeddedt.embeddium.impl.Celeritas;
import org.embeddedt.embeddium.impl.model.color.ColorProviderRegistry;
import org.embeddedt.embeddium.impl.model.light.DiffuseProvider;
import org.embeddedt.embeddium.impl.model.light.LightPipelineProvider;
//? if forgelike && <1.19 {
/*import org.embeddedt.embeddium.impl.render.EmbeddiumRenderLayerCache;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
*///?}
import org.embeddedt.embeddium.impl.model.quad.ArrayLightDataCache;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.embeddedt.embeddium.impl.modern.util.ModernQuadFacing;
import org.embeddedt.embeddium.impl.util.WorldUtil;
import org.embeddedt.embeddium.impl.world.WorldSlice;
import org.embeddedt.embeddium.impl.world.cloned.ChunkRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

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
    //? if shaders {
    @Getter
    private final SpecialBlockRenderer specialBlockRenderer;
    //?}

    @Getter
    //? if <26.1 {
    private final BlockModelShaper blockModels;
    //?} else
    /*private final BlockStateModelSet blockModels;*/

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

        //? if shaders {
        boolean directionalShadingOff = WorldRenderingSettings.INSTANCE.shouldDisableDirectionalShading();
        //?} else
        /*boolean directionalShadingOff = false;*/

        LightPipelineProvider lightPipelineProvider = new LightPipelineProvider(this.lightDataCache,
                directionalShadingOff ? DiffuseProvider.NONE : new DiffuseProvider() {
                    @Override
                    public float getDiffuse(float normalX, float normalY, float normalZ, boolean shade) {
                        //? if >=1.21.11 {
                        /*var cardinalLighting = world.cardinalLighting();
                        if (!shade) {
                            return cardinalLighting.up();
                        }
                        float diffuse = 0;
                        if (normalX != 0) {
                            diffuse += (normalX > 0 ? cardinalLighting.east() : cardinalLighting.west()) * (normalX * normalX);
                        }
                        if (normalY != 0) {
                            diffuse += (normalY > 0 ? cardinalLighting.up() : cardinalLighting.down()) * (normalY * normalY);
                        }
                        if (normalZ != 0) {
                            diffuse += (normalZ > 0 ? cardinalLighting.south() : cardinalLighting.north()) * (normalZ * normalZ);
                        }
                        return Math.min(diffuse, 1f);
                        *///?} else if forgelike && >=1.19 {
                        return world.getShade(normalX, normalY, normalZ, shade);
                        //?} else if forgelike && <1.19 {
                        /*if (!shade) return world.effects().constantAmbientLight() ? 0.9f : 1.0f;
                        return net.minecraftforge.client.model.pipeline.LightUtil.diffuseLight(normalX, normalY, normalZ);
                        *///?} else {
                        /*throw new IllegalStateException();
                        *///?}
                    }

                    @Override
                    public float getDiffuse(ModelQuadFacing lightFace, boolean shade) {
                        return WorldUtil.getShade(world, ModernQuadFacing.toDirection(lightFace), shade);
                    }
                },
                Celeritas.options().quality.useQuadNormalsForShading);

        var colorRegistry = new ColorProviderRegistry(client.getBlockColors());

        this.blockRenderer = new BlockRenderer(new BlockRenderer.BlockRendererConfig(colorRegistry, lightPipelineProvider
                //? if shaders
                , WorldRenderingSettings.INSTANCE.getBlockTypeIds()
        ));
        this.fluidRenderer = new FluidRenderer(colorRegistry, lightPipelineProvider);
        this.lightPipelineProvider = lightPipelineProvider;
        //? if shaders
        this.specialBlockRenderer = new SpecialBlockRenderer();

        //? if <26.1 {
        this.blockModels = client.getModelManager().getBlockModelShaper();
        //?} else
        /*this.blockModels = client.getModelManager().getBlockStateModelSet();*/
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
        this.lightDataCache.reset(context.getOrigin().minBlockX(), context.getOrigin().minBlockY(), context.getOrigin().minBlockZ());
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
