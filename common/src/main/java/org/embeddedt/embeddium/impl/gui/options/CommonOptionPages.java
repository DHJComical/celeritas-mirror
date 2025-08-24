package org.embeddedt.embeddium.impl.gui.options;

import org.taumc.celeritas.api.options.control.ControlValueFormatter;
import org.taumc.celeritas.api.options.control.CyclingControl;
import org.taumc.celeritas.api.options.control.SliderControl;
import org.taumc.celeritas.api.options.control.TickBoxControl;
import org.embeddedt.embeddium.impl.gui.SodiumGameOptions;
import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.embeddedt.embeddium.impl.render.ShaderModBridge;
import org.embeddedt.embeddium.impl.render.chunk.compile.executor.ChunkBuilder;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.AsyncOcclusionMode;
import org.taumc.celeritas.api.options.structure.*;

import java.util.ArrayList;
import java.util.List;

public class CommonOptionPages {
    public static OptionGroup sortingGroup(SodiumGameOptions sodiumOpts) {
        return OptionGroup.createBuilder()
                .setId(StandardOptions.Group.SORTING)
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setId(StandardOptions.Option.TRANSLUCENT_FACE_SORTING.cast())
                        .setName(TextComponent.translatable("sodium.options.translucent_face_sorting.name"))
                        .setTooltip(TextComponent.translatable("sodium.options.translucent_face_sorting.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.VARIES)
                        .setBinding((opts, value) -> opts.performance.useTranslucentFaceSorting = value, opts -> opts.performance.useTranslucentFaceSorting)
                        .setEnabled(!ShaderModBridge.isNvidiumEnabled())
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .add(OptionImpl.createBuilder(int.class, sodiumOpts)
                        .setId(StandardOptions.Option.CHUNK_FADE_IN_DURATION.cast())
                        .setName(TextComponent.translatable("celeritas.options.chunk_fade_in_duration.name"))
                        .setTooltip(TextComponent.translatable("celeritas.options.chunk_fade_in_duration.tooltip"))
                        .setControl(o -> new SliderControl(o, 0, 2000, 100, ControlValueFormatter.translateVariable("celeritas.options.chunk_fade_in_duration.value")))
                        .setImpact(OptionImpact.LOW)
                        .setBinding((opts, value) -> opts.quality.chunkFadeInDuration = value, opts -> opts.quality.chunkFadeInDuration)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .build();
    }

    public static OptionPage performance(SodiumGameOptions sodiumOpts) {
        List<OptionGroup> groups = new ArrayList<>();

        groups.add(OptionGroup.createBuilder()
                .setId(StandardOptions.Group.CHUNK_UPDATES)
                .add(OptionImpl.createBuilder(int.class, sodiumOpts)
                        .setId(StandardOptions.Option.CHUNK_UPDATE_THREADS.cast())
                        .setName(TextComponent.translatable("sodium.options.chunk_update_threads.name"))
                        .setTooltip(TextComponent.translatable("sodium.options.chunk_update_threads.tooltip"))
                        .setControl(o -> new SliderControl(o, 0, ChunkBuilder.getMaxThreadCount(), 1, ControlValueFormatter.quantityOrDisabled("threads", "Default")))
                        .setImpact(OptionImpact.HIGH)
                        .setBinding((opts, value) -> opts.performance.chunkBuilderThreads = value, opts -> opts.performance.chunkBuilderThreads)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build()
                )
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setId(StandardOptions.Option.DEFFER_CHUNK_UPDATES.cast())
                        .setName(TextComponent.translatable("sodium.options.always_defer_chunk_updates.name"))
                        .setTooltip(TextComponent.translatable("sodium.options.always_defer_chunk_updates.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.HIGH)
                        .setBinding((opts, value) -> opts.performance.alwaysDeferChunkUpdates = value, opts -> opts.performance.alwaysDeferChunkUpdates)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_UPDATE)
                        .build())
                .add(OptionImpl.createBuilder(AsyncOcclusionMode.class, sodiumOpts)
                        .setId(StandardOptions.Option.ASYNC_GRAPH_SEARCH.cast())
                        .setName(TextComponent.translatable("celeritas.options.async_graph_search.name"))
                        .setTooltip(TextComponent.translatable("celeritas.options.async_graph_search.tooltip"))
                        .setControl(o -> new CyclingControl<>(o, AsyncOcclusionMode.class, new TextComponent[] { TextComponent.literal("Off"), TextComponent.literal("Only Shadows"), TextComponent.literal("Everything") }))
                        .setImpact(OptionImpact.MEDIUM)
                        .setBinding((opts, value) -> opts.performance.asyncOcclusionMode = value, opts -> opts.performance.asyncOcclusionMode)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .build()
        );

        groups.add(OptionGroup.createBuilder()
                .setId(StandardOptions.Group.RENDERING_CULLING)
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setId(StandardOptions.Option.BLOCK_FACE_CULLING.cast())
                        .setName(TextComponent.translatable("sodium.options.use_block_face_culling.name"))
                        .setTooltip(TextComponent.translatable("sodium.options.use_block_face_culling.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.MEDIUM)
                        .setBinding((opts, value) -> opts.performance.useBlockFaceCulling = value, opts -> opts.performance.useBlockFaceCulling)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build()
                )
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setId(StandardOptions.Option.COMPACT_VERTEX_FORMAT.cast())
                        .setName(TextComponent.translatable("sodium.options.use_compact_vertex_format.name"))
                        .setTooltip(TextComponent.translatable("sodium.options.use_compact_vertex_format.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.MEDIUM)
                        .setBinding((opts, value) -> {
                            opts.performance.useCompactVertexFormat = value;
                        }, opts -> opts.performance.useCompactVertexFormat)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build()
                )
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setId(StandardOptions.Option.FOG_OCCLUSION.cast())
                        .setName(TextComponent.translatable("sodium.options.use_fog_occlusion.name"))
                        .setTooltip(TextComponent.translatable("sodium.options.use_fog_occlusion.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.performance.useFogOcclusion = value, opts -> opts.performance.useFogOcclusion)
                        .setImpact(OptionImpact.MEDIUM)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_UPDATE)
                        .build()
                )
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setId(StandardOptions.Option.ENTITY_CULLING.cast())
                        .setName(TextComponent.translatable("sodium.options.use_entity_culling.name"))
                        .setTooltip(TextComponent.translatable("sodium.options.use_entity_culling.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.MEDIUM)
                        .setBinding((opts, value) -> opts.performance.useEntityCulling = value, opts -> opts.performance.useEntityCulling)
                        .build()
                )
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setId(StandardOptions.Option.ANIMATE_VISIBLE_TEXTURES.cast())
                        .setName(TextComponent.translatable("sodium.options.animate_only_visible_textures.name"))
                        .setTooltip(TextComponent.translatable("sodium.options.animate_only_visible_textures.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.HIGH)
                        .setBinding((opts, value) -> opts.performance.animateOnlyVisibleTextures = value, opts -> opts.performance.animateOnlyVisibleTextures)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_UPDATE)
                        .build()
                )
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setId(StandardOptions.Option.RENDER_PASS_CONSOLIDATION.cast())
                        .setName(TextComponent.translatable("embeddium.options.use_render_pass_consolidation.name"))
                        .setTooltip(TextComponent.translatable("embeddium.options.use_render_pass_consolidation.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.LOW)
                        .setBinding((opts, value) -> opts.performance.useRenderPassConsolidation = value, opts -> opts.performance.useRenderPassConsolidation)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setId(StandardOptions.Option.RENDER_PASS_OPTIMIZATION.cast())
                        .setName(TextComponent.translatable("embeddium.options.use_render_pass_optimization.name"))
                        .setTooltip(TextComponent.translatable("embeddium.options.use_render_pass_optimization.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.LOW)
                        .setBinding((opts, value) -> opts.performance.useRenderPassOptimization = value, opts -> opts.performance.useRenderPassOptimization)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                //? if <1.21.2 {
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setId(StandardOptions.Option.USE_FASTER_CLOUDS.cast())
                        .setName(TextComponent.translatable("embeddium.options.use_faster_clouds.name"))
                        .setTooltip(TextComponent.translatable("embeddium.options.use_faster_clouds.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.LOW)
                        .setBinding((opts, value) -> opts.performance.useFasterClouds = value, opts -> opts.performance.useFasterClouds)
                        .build())
                //?}
                .build());

        return new OptionPage(StandardOptions.Pages.PERFORMANCE, TextComponent.translatable("sodium.options.pages.performance"), List.copyOf(groups));
    }
}
