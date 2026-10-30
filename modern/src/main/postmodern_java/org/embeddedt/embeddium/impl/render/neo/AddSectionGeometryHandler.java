package org.embeddedt.embeddium.impl.render.neo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.config.NeoForgeClientConfig;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.embeddedt.embeddium.api.ChunkMeshEvent;
import org.embeddedt.embeddium.impl.Celeritas;

@EventBusSubscriber(modid = Celeritas.MODID, value = Dist.CLIENT)
public class AddSectionGeometryHandler {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent e) {
        ChunkMeshEvent.BUS.addListener(AddSectionGeometryHandler::onChunkMesh);
    }

    public static void onChunkMesh(ChunkMeshEvent meshEvent) {
        AddSectionGeometryEvent geometryEvent = new AddSectionGeometryEvent(meshEvent.getSectionOrigin().origin(), meshEvent.getWorld());
        NeoForge.EVENT_BUS.post(geometryEvent);
        var renderers = geometryEvent.getAdditionalRenderers();
        if (renderers.isEmpty()) {
            return;
        }

        var minecraft = Minecraft.getInstance();
        boolean ambientOcclusion = minecraft.options.ambientOcclusion().get();
        boolean enhancedLighting = NeoForgeClientConfig.INSTANCE.enhancedLighting.getAsBoolean();
        var blockColors = minecraft.getBlockColors();

        meshEvent.addMeshAppender(ctx -> {
            var blockRenderer = getBlockRenderer(ambientOcclusion, enhancedLighting, blockColors);
            var context = new AddSectionGeometryEvent.SectionRenderingContext(ctx.vertexConsumerProvider(), ctx.blockRenderView(), blockRenderer);
            for (var renderer : renderers) {
                renderer.render(context);
            }
        });
    }

    // ModelBlockRenderer holds mutable lighting and tint state, so each meshing thread needs its own.
    // Both flags are baked in at construction (enhanced lighting picks the lighter implementation).
    private record CachedBlockRenderer(boolean ambientOcclusion, boolean enhancedLighting, ModelBlockRenderer renderer) {}

    private static final ThreadLocal<CachedBlockRenderer> BLOCK_RENDERER = new ThreadLocal<>();

    private static ModelBlockRenderer getBlockRenderer(boolean ambientOcclusion, boolean enhancedLighting, BlockColors blockColors) {
        var cached = BLOCK_RENDERER.get();
        if (cached == null || cached.ambientOcclusion() != ambientOcclusion || cached.enhancedLighting() != enhancedLighting) {
            cached = new CachedBlockRenderer(ambientOcclusion, enhancedLighting, new ModelBlockRenderer(ambientOcclusion, true, blockColors));
            BLOCK_RENDERER.set(cached);
        }
        return cached.renderer();
    }
}
