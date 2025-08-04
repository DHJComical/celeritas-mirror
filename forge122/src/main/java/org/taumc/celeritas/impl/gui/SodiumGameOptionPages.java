package org.taumc.celeritas.impl.gui;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.text.ITextComponent;
import org.embeddedt.embeddium.impl.gl.arena.staging.MappedStagingBuffer;
import org.embeddedt.embeddium.impl.gl.device.RenderDevice;
import org.embeddedt.embeddium.impl.render.chunk.compile.executor.ChunkBuilder;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.AsyncOcclusionMode;
import org.lwjgl.opengl.Display;
import org.taumc.celeritas.api.options.named.*;
import org.taumc.celeritas.api.options.structure.*;
import org.taumc.celeritas.impl.compat.modernui.MuiGuiScaleHook;
import org.taumc.celeritas.api.options.control.ControlValueFormatter;
import org.taumc.celeritas.api.options.control.CyclingControl;
import org.taumc.celeritas.api.options.control.SliderControl;
import org.taumc.celeritas.api.options.control.TickBoxControl;
import org.taumc.celeritas.api.options.storage.MinecraftOptionsStorage;
import org.taumc.celeritas.impl.gui.options.storage.SodiumOptionsStorage;
import net.minecraft.client.*;
import org.taumc.celeritas.impl.util.ComponentUtil;

import java.util.ArrayList;
import java.util.List;

public class SodiumGameOptionPages {
    private static final SodiumOptionsStorage sodiumOpts = new SodiumOptionsStorage();
    private static final MinecraftOptionsStorage vanillaOpts = new MinecraftOptionsStorage();

    private static int computeMaxRangeForRenderDistance(@SuppressWarnings("SameParameterValue") int injectedRenderDistance) {
        return injectedRenderDistance;
    }

    public static OptionPage general() {
        List<OptionGroup> groups = new ArrayList<>();
        groups.add(OptionGroup.createBuilder()
                .setId(StandardOptions.Group.RENDERING)
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setId(StandardOptions.Option.RENDER_DISTANCE)
                        .setName(ComponentUtil.literal(I18n.format("options.renderDistance")))
                        .setTooltip(ComponentUtil.translatable("sodium.options.view_distance.tooltip"))
                        .setControl(option -> new SliderControl(option, 2, computeMaxRangeForRenderDistance(32), 1, ControlValueFormatter.translateVariable("options.chunks")))
                        .setBinding((options, value) -> options.renderDistanceChunks = value, options -> options.renderDistanceChunks)
                        .setImpact(OptionImpact.HIGH)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setId(StandardOptions.Option.BRIGHTNESS)
                        .setName(ComponentUtil.translatable("options.gamma"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.brightness.tooltip"))
                        .setControl(opt -> new SliderControl(opt, 0, 100, 1, ControlValueFormatter.brightness()))
                        .setBinding((opts, value) -> opts.gammaSetting = (float) (value * 0.01D), (opts) -> (int) (opts.gammaSetting / 0.01D))
                        .build())
                .build());

        groups.add(OptionGroup.createBuilder()
                .setId(StandardOptions.Group.WINDOW)
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setId(StandardOptions.Option.GUI_SCALE)
                        .setName(ComponentUtil.translatable("options.guiScale"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.gui_scale.tooltip"))
                        .setControl(option -> new SliderControl(option, 0, MuiGuiScaleHook.getMaxGuiScale(), 1, ControlValueFormatter.guiScale()))
                        .setBinding((opts, value) -> {
                            opts.guiScale = value;

                            Minecraft mc = Minecraft.getMinecraft();
                            mc.resize(mc.displayWidth, mc.displayHeight);
                        }, opts -> opts.guiScale)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setId(StandardOptions.Option.FULLSCREEN)
                        .setName(ComponentUtil.translatable("options.fullscreen"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.fullscreen.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> {
                            opts.fullScreen = value;

                            Minecraft client = Minecraft.getMinecraft();

                            if (client.isFullScreen() != opts.fullScreen) {
                                client.toggleFullscreen();

                                // The client might not be able to enter full-screen mode
                                opts.fullScreen = client.isFullScreen();
                            }
                        }, (opts) -> opts.fullScreen)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setId(StandardOptions.Option.VSYNC)
                        .setName(ComponentUtil.translatable("options.vsync"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.v_sync.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> {
                            opts.enableVsync = value;
                            Display.setVSyncEnabled(opts.enableVsync);
                        }, opts -> opts.enableVsync)
                        .setImpact(OptionImpact.VARIES)
                        .build())
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setId(StandardOptions.Option.MAX_FRAMERATE)
                        .setName(ComponentUtil.translatable("options.framerateLimit"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.fps_limit.tooltip"))
                        .setControl(option -> new SliderControl(option, 10, 260, 10, ControlValueFormatter.fpsLimit()))
                        .setBinding((opts, value) -> opts.limitFramerate = value, opts -> opts.limitFramerate)
                        .build())
                .build());

        groups.add(OptionGroup.createBuilder()
                .setId(StandardOptions.Group.INDICATORS)
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setId(StandardOptions.Option.VIEW_BOBBING)
                        .setName(ComponentUtil.translatable("options.viewBobbing"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.view_bobbing.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.viewBobbing = value, opts -> opts.viewBobbing)
                        .build())
                .add(OptionImpl.createBuilder(AttackIndicator.class, vanillaOpts)
                        .setId(StandardOptions.Option.ATTACK_INDICATOR)
                        .setName(ComponentUtil.translatable("options.attackIndicator"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.attack_indicator.tooltip"))
                        .setControl(opts -> new CyclingControl<>(opts, AttackIndicator.class, new ITextComponent[] {
                                ComponentUtil.translatable(AttackIndicator.OFF.getTranslationKey()),
                                ComponentUtil.translatable(AttackIndicator.CROSSHAIR.getTranslationKey()),
                                ComponentUtil.translatable(AttackIndicator.HOTBAR.getTranslationKey()) }))
                        .setBinding((opts, value) -> opts.attackIndicator = value.getId(), (opts) -> AttackIndicator.byId(opts.attackIndicator))
                        .build())
                .build());

        return new OptionPage(ComponentUtil.translatable("stat.generalButton"), ImmutableList.copyOf(groups));
    }

    public static OptionPage quality() {
        List<OptionGroup> groups = new ArrayList<>();

        groups.add(OptionGroup.createBuilder()
                .setId(StandardOptions.Group.GRAPHICS)
                .add(OptionImpl.createBuilder(GraphicsStatus.class, vanillaOpts)
                        .setId(StandardOptions.Option.GRAPHICS_MODE)
                        .setName(ComponentUtil.translatable("options.graphics"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.graphics_quality.tooltip"))
                        .setControl(option -> new CyclingControl<>(option, GraphicsStatus.class, new ITextComponent[] {
                                ComponentUtil.translatable(GraphicsStatus.FAST.getTranslationKey()),
                                ComponentUtil.translatable(GraphicsStatus.FANCY.getTranslationKey()) }))
                        .setBinding((opts, value) -> opts.fancyGraphics = value.getValue(), opts -> GraphicsStatus.fromBoolean(opts.fancyGraphics))
                        .setImpact(OptionImpact.HIGH)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .build());

        groups.add(OptionGroup.createBuilder()
                .setId(StandardOptions.Group.DETAILS)
                .add(OptionImpl.createBuilder(CloudStatus.class, vanillaOpts)
                        .setId(StandardOptions.Option.CLOUDS)
                        .setName(ComponentUtil.translatable("options.renderClouds"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.clouds_quality.tooltip"))
                        .setControl(option -> new CyclingControl<>(option, CloudStatus.class, new ITextComponent[] {
                                ComponentUtil.translatable(CloudStatus.OFF.getTranslationKey()),
                                ComponentUtil.translatable(CloudStatus.FAST.getTranslationKey()),
                                ComponentUtil.translatable(CloudStatus.FANCY.getTranslationKey()) }))
                        .setBinding((opts, value) -> {
                            opts.clouds = value.getId();
                        }, opts -> {
                            return CloudStatus.byId(opts.clouds);
                        })
                        .setImpact(OptionImpact.LOW)
                        .build())
                .add(OptionImpl.createBuilder(SodiumGameOptions.GraphicsQuality.class, sodiumOpts)
                        .setId(StandardOptions.Option.WEATHER)
                        .setName(ComponentUtil.translatable("soundCategory.weather"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.weather_quality.tooltip"))
                        .setControl(option -> new CyclingControl<>(option, SodiumGameOptions.GraphicsQuality.class))
                        .setBinding((opts, value) -> opts.quality.weatherQuality = value, opts -> opts.quality.weatherQuality)
                        .setImpact(OptionImpact.MEDIUM)
                        .build())
                .add(OptionImpl.createBuilder(SodiumGameOptions.GraphicsQuality.class, sodiumOpts)
                        .setId(StandardOptions.Option.LEAVES)
                        .setName(ComponentUtil.translatable("sodium.options.leaves_quality.name"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.leaves_quality.tooltip"))
                        .setControl(option -> new CyclingControl<>(option, SodiumGameOptions.GraphicsQuality.class))
                        .setBinding((opts, value) -> opts.quality.leavesQuality = value, opts -> opts.quality.leavesQuality)
                        .setImpact(OptionImpact.MEDIUM)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .add(OptionImpl.createBuilder(ParticleMode.class, vanillaOpts)
                        .setId(StandardOptions.Option.PARTICLES)
                        .setName(ComponentUtil.translatable("options.particles"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.particle_quality.tooltip"))
                        .setControl(option -> new CyclingControl<>(option, ParticleMode.class, new ITextComponent[] {
                                ComponentUtil.translatable(ParticleMode.ALL.getTranslationKey()),
                                ComponentUtil.translatable(ParticleMode.DECREASED.getTranslationKey()),
                                ComponentUtil.translatable(ParticleMode.MINIMAL.getTranslationKey()) }))
                        .setBinding((opts, value) -> opts.particleSetting = value.ordinal(), (opts) -> ParticleMode.byId(opts.particleSetting))
                        .setImpact(OptionImpact.MEDIUM)
                        .build())
                .add(OptionImpl.createBuilder(SmoothLighting.class, vanillaOpts)
                        .setId(StandardOptions.Option.SMOOTH_LIGHT)
                        .setName(ComponentUtil.translatable("options.ao"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.smooth_lighting.tooltip"))
                        .setControl(option -> new CyclingControl<>(option, SmoothLighting.class, new ITextComponent[] {
                                ComponentUtil.translatable(SmoothLighting.OFF.getTranslationKey()),
                                ComponentUtil.translatable(SmoothLighting.MINIMAL.getTranslationKey()),
                                ComponentUtil.translatable(SmoothLighting.MAXIMUM.getTranslationKey()) }))
                        .setBinding((opts, value) -> opts.ambientOcclusion = value.getId(), opts -> SmoothLighting.byId(opts.ambientOcclusion))
                        .setImpact(OptionImpact.LOW)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setId(StandardOptions.Option.ENTITY_SHADOWS)
                        .setName(ComponentUtil.translatable("options.entityShadows"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.entity_shadows.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.entityShadows = value, opts -> opts.entityShadows)
                        .setImpact(OptionImpact.LOW)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setId(StandardOptions.Option.VIGNETTE)
                        .setName(ComponentUtil.translatable("sodium.options.vignette.name"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.vignette.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.quality.enableVignette = value, opts -> opts.quality.enableVignette)
                        .setImpact(OptionImpact.LOW)
                        .build())
                .build());


        groups.add(OptionGroup.createBuilder()
                .setId(StandardOptions.Group.MIPMAPS)
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setId(StandardOptions.Option.MIPMAP_LEVEL)
                        .setName(ComponentUtil.translatable("options.mipmapLevels"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.mipmap_levels.tooltip"))
                        .setControl(option -> new SliderControl(option, 0, 4, 1, ControlValueFormatter.multiplier()))
                        .setBinding((opts, value) -> opts.mipmapLevels = value, opts -> opts.mipmapLevels)
                        .setImpact(OptionImpact.MEDIUM)
                        .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD)
                        .build())
                .build());

        groups.add(OptionGroup.createBuilder()
                .setId(StandardOptions.Group.SORTING)
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setId(StandardOptions.Option.TRANSLUCENT_FACE_SORTING)
                        .setName(ComponentUtil.translatable("sodium.options.translucent_face_sorting.name"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.translucent_face_sorting.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.VARIES)
                        .setBinding((opts, value) -> opts.performance.useTranslucentFaceSorting = value, opts -> opts.performance.useTranslucentFaceSorting)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .build());

        return new OptionPage(ComponentUtil.translatable("sodium.options.pages.quality"), ImmutableList.copyOf(groups));
    }

    public static OptionPage performance() {
        List<OptionGroup> groups = new ArrayList<>();

        groups.add(OptionGroup.createBuilder()
                .setId(StandardOptions.Group.CHUNK_UPDATES)
                .add(OptionImpl.createBuilder(int.class, sodiumOpts)
                        .setId(StandardOptions.Option.CHUNK_UPDATE_THREADS)
                        .setName(ComponentUtil.translatable("sodium.options.chunk_update_threads.name"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.chunk_update_threads.tooltip"))
                        .setControl(o -> new SliderControl(o, 0, ChunkBuilder.getMaxThreadCount(), 1, ControlValueFormatter.quantityOrDisabled("threads", "Default")))
                        .setImpact(OptionImpact.HIGH)
                        .setBinding((opts, value) -> opts.performance.chunkBuilderThreads = value, opts -> opts.performance.chunkBuilderThreads)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build()
                )
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setId(StandardOptions.Option.DEFFER_CHUNK_UPDATES)
                        .setName(ComponentUtil.translatable("sodium.options.always_defer_chunk_updates.name"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.always_defer_chunk_updates.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.HIGH)
                        .setBinding((opts, value) -> opts.performance.alwaysDeferChunkUpdates = value, opts -> opts.performance.alwaysDeferChunkUpdates)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_UPDATE)
                        .build())
                .add(OptionImpl.createBuilder(AsyncOcclusionMode.class, sodiumOpts)
                        .setId(StandardOptions.Option.ASYNC_GRAPH_SEARCH)
                        .setName(ComponentUtil.translatable("celeritas.options.async_graph_search.name"))
                        .setTooltip(ComponentUtil.translatable("celeritas.options.async_graph_search.tooltip"))
                        .setControl(o -> new CyclingControl<>(o, AsyncOcclusionMode.class, new ITextComponent[] {
                                ComponentUtil.literal("Off"),
                                ComponentUtil.literal("Only Shadows"),
                                ComponentUtil.literal("Everything") }))
                        .setImpact(OptionImpact.MEDIUM)
                        .setBinding((opts, value) -> opts.performance.asyncOcclusionMode = value, opts -> opts.performance.asyncOcclusionMode)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .build()
        );

        groups.add(OptionGroup.createBuilder()
                .setId(StandardOptions.Group.RENDERING_CULLING)
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setId(StandardOptions.Option.BLOCK_FACE_CULLING)
                        .setName(ComponentUtil.translatable("sodium.options.use_block_face_culling.name"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.use_block_face_culling.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.MEDIUM)
                        .setBinding((opts, value) -> opts.performance.useBlockFaceCulling = value, opts -> opts.performance.useBlockFaceCulling)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build()
                )
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setId(StandardOptions.Option.COMPACT_VERTEX_FORMAT)
                        .setName(ComponentUtil.translatable("sodium.options.use_compact_vertex_format.name"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.use_compact_vertex_format.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.MEDIUM)
                        .setBinding((opts, value) -> {
                            opts.performance.useCompactVertexFormat = value;
                        }, opts -> opts.performance.useCompactVertexFormat)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build()
                )
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setId(StandardOptions.Option.FOG_OCCLUSION)
                        .setName(ComponentUtil.translatable("sodium.options.use_fog_occlusion.name"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.use_fog_occlusion.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.performance.useFogOcclusion = value, opts -> opts.performance.useFogOcclusion)
                        .setImpact(OptionImpact.MEDIUM)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_UPDATE)
                        .build()
                )
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setId(StandardOptions.Option.ENTITY_CULLING)
                        .setName(ComponentUtil.translatable("sodium.options.use_entity_culling.name"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.use_entity_culling.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.MEDIUM)
                        .setBinding((opts, value) -> opts.performance.useEntityCulling = value, opts -> opts.performance.useEntityCulling)
                        .build()
                )
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setId(StandardOptions.Option.ANIMATE_VISIBLE_TEXTURES)
                        .setName(ComponentUtil.translatable("sodium.options.animate_only_visible_textures.name"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.animate_only_visible_textures.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.HIGH)
                        .setBinding((opts, value) -> opts.performance.animateOnlyVisibleTextures = value, opts -> opts.performance.animateOnlyVisibleTextures)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_UPDATE)
                        .build()
                )
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setId(StandardOptions.Option.RENDER_PASS_CONSOLIDATION)
                        .setName(ComponentUtil.translatable("celeritas.options.use_render_pass_consolidation.name"))
                        .setTooltip(ComponentUtil.translatable("celeritas.options.use_render_pass_consolidation.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.LOW)
                        .setBinding((opts, value) -> opts.performance.useRenderPassConsolidation = value, opts -> opts.performance.useRenderPassConsolidation)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setId(StandardOptions.Option.RENDER_PASS_OPTIMIZATION)
                        .setName(ComponentUtil.translatable("celeritas.options.use_render_pass_optimization.name"))
                        .setTooltip(ComponentUtil.translatable("celeritas.options.use_render_pass_optimization.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.LOW)
                        .setBinding((opts, value) -> opts.performance.useRenderPassOptimization = value, opts -> opts.performance.useRenderPassOptimization)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .build());

        return new OptionPage(ComponentUtil.translatable("sodium.options.pages.performance"), ImmutableList.copyOf(groups));
    }

    public static OptionPage advanced() {
        List<OptionGroup> groups = new ArrayList<>();

        groups.add(OptionGroup.createBuilder()
                .setId(StandardOptions.Group.CPU_SAVING)
                .add(OptionImpl.createBuilder(int.class, sodiumOpts)
                        .setId(StandardOptions.Option.CPU_FRAMES_AHEAD)
                        .setName(ComponentUtil.translatable("sodium.options.cpu_render_ahead_limit.name"))
                        .setTooltip(ComponentUtil.translatable("sodium.options.cpu_render_ahead_limit.tooltip"))
                        .setControl(opt -> new SliderControl(opt, 0, 9, 1, ControlValueFormatter.translateVariable("sodium.options.cpu_render_ahead_limit.value")))
                        .setBinding((opts, value) -> opts.advanced.cpuRenderAheadLimit = value, opts -> opts.advanced.cpuRenderAheadLimit)
                        .build()
                )
                .build());

        return new OptionPage(ComponentUtil.translatable("sodium.options.pages.advanced"), ImmutableList.copyOf(groups));
    }

    public static OptionStorage<GameSettings> getVanillaOpts() {
        return vanillaOpts;
    }

    public static OptionStorage<SodiumGameOptions> getSodiumOpts() {
        return sodiumOpts;
    }
}
