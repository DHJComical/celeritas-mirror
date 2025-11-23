package org.embeddedt.embeddium.impl.gui.modern;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
//? if forge
import net.minecraftforge.common.ForgeConfig;
//? if <1.19
/*import net.minecraft.client.Option;*/
import org.embeddedt.embeddium.impl.Celeritas;
import org.embeddedt.embeddium.impl.compat.modernui.MuiGuiScaleHook;
//? if >=1.18
import org.embeddedt.embeddium.impl.compatibility.workarounds.Workarounds;
import org.embeddedt.embeddium.impl.gl.arena.staging.MappedStagingBuffer;
import org.embeddedt.embeddium.impl.gl.device.RenderDevice;
import org.taumc.celeritas.api.options.control.ControlValueFormatter;
import org.taumc.celeritas.api.options.control.CyclingControl;
import org.taumc.celeritas.api.options.control.SliderControl;
import org.taumc.celeritas.api.options.control.TickBoxControl;
import org.embeddedt.embeddium.impl.gui.SodiumGameOptions;
import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.embeddedt.embeddium.impl.gui.framework.TextFormattingStyle;
import org.embeddedt.embeddium.impl.gui.options.CommonOptionPages;
import net.minecraft.client.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.taumc.celeritas.api.options.structure.*;

import java.util.ArrayList;
import java.util.List;

public class SodiumGameOptionPages {
    private static final MinecraftOptionsStorage vanillaOpts = new MinecraftOptionsStorage();

    private static int computeMaxRangeForRenderDistance(@SuppressWarnings("SameParameterValue") int injectedRenderDistance) {
        //? if >=1.19 {
        if(vanillaOpts.getData().renderDistance().values() instanceof OptionInstance.IntRange range) {
            injectedRenderDistance = Math.max(injectedRenderDistance, range.maxInclusive());
        }
        //?}
        return injectedRenderDistance;
    }

    public static OptionPage general() {
        List<OptionGroup> groups = new ArrayList<>();

        groups.add(OptionGroup.createBuilder()
                .setId(StandardOptions.Group.RENDERING)
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setId(StandardOptions.Option.RENDER_DISTANCE.cast())
                        .setName(TextComponent.translatable("options.renderDistance"))
                        .setTooltip(TextComponent.translatable("sodium.options.view_distance.tooltip"))
                        .setControl(option -> new SliderControl(option, 2, computeMaxRangeForRenderDistance(32), 1, ControlValueFormatter.translateVariable("options.chunks")))
                        .setBinding((options, value) -> options.renderDistance/*? if >=1.19 {*/().set/*?} else {*//*=*//*?}*/(value), options -> options.renderDistance/*? if >=1.19 {*/().get()/*?}*/)
                        .setImpact(OptionImpact.HIGH)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                //? if >=1.18 {
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setId(StandardOptions.Option.SIMULATION_DISTANCE.cast())
                        .setName(TextComponent.translatable("options.simulationDistance"))
                        .setTooltip(TextComponent.translatable("sodium.options.simulation_distance.tooltip"))
                        .setControl(option -> new SliderControl(option, 5, 32, 1, ControlValueFormatter.translateVariable("options.chunks")))
                        .setBinding((options, value) -> options.simulationDistance/*? if >=1.19 {*/().set/*?} else {*//*=*//*?}*/(value), options -> options.simulationDistance/*? if >=1.19 {*/().get()/*?}*/)
                        .setImpact(OptionImpact.HIGH)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                //?}
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setId(StandardOptions.Option.BRIGHTNESS.cast())
                        .setName(TextComponent.translatable("options.gamma"))
                        .setTooltip(TextComponent.translatable("sodium.options.brightness.tooltip"))
                        .setControl(opt -> new SliderControl(opt, 0, 100, 1, ControlValueFormatter.brightness()))
                        .setBinding((opts, value) -> opts.gamma/*? if >=1.19 {*/().set/*?} else {*//*=*//*?}*/(value * 0.01D), (opts) -> (int) (opts.gamma/*? if >=1.19 {*/().get()/*?}*/ / 0.01D))
                        .build())
                .build());

        groups.add(OptionGroup.createBuilder()
                .setId(StandardOptions.Group.WINDOW)
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setId(StandardOptions.Option.GUI_SCALE.cast())
                        .setName(TextComponent.translatable("options.guiScale"))
                        .setTooltip(TextComponent.translatable("sodium.options.gui_scale.tooltip"))
                        .setControl(option -> new SliderControl(option, 0, MuiGuiScaleHook.getMaxGuiScale(), 1, ControlValueFormatter.guiScale()))
                        .setBinding((opts, value) -> {
                            opts.guiScale/*? if >=1.19 {*/().set/*?} else {*//*=*//*?}*/(value);

                            Minecraft client = Minecraft.getInstance();
                            client.resizeDisplay();
                        }, opts -> opts.guiScale/*? if >=1.19 {*/().get()/*?}*/)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setId(StandardOptions.Option.FULLSCREEN.cast())
                        .setName(TextComponent.translatable("options.fullscreen"))
                        .setTooltip(TextComponent.translatable("sodium.options.fullscreen.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> {
                            opts.fullscreen/*? if >=1.19 {*/().set/*?} else {*//*=*//*?}*/(value);

                            Minecraft client = Minecraft.getInstance();
                            Window window = client.getWindow();

                            if (window != null && window.isFullscreen() != opts.fullscreen/*? if >=1.19 {*/().get()/*?}*/) {
                                window.toggleFullScreen();

                                // The client might not be able to enter full-screen mode
                                opts.fullscreen/*? if >=1.19 {*/().set/*?} else {*//*=*//*?}*/(window.isFullscreen());
                            }
                        }, (opts) -> opts.fullscreen/*? if >=1.19 {*/().get()/*?}*/)
                        .build())
                .addConditionally(!FullscreenResolutionHelper.isFullscreenResAlreadyAdded(), FullscreenResolutionHelper::createFullScreenResolutionOption)
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setId(StandardOptions.Option.VSYNC.cast())
                        .setName(TextComponent.translatable("options.vsync"))
                        .setTooltip(TextComponent.translatable("sodium.options.v_sync.tooltip"))
                        .setControl(TickBoxControl::new)
                        //? if >=1.19 {
                        .setBinding(new VanillaBooleanOptionBinding(Minecraft.getInstance().options.enableVsync()))
                        //?} else
                        /*.setBinding(new VanillaBooleanOptionBinding(Option.ENABLE_VSYNC))*/
                        .setImpact(OptionImpact.VARIES)
                        .build())
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setId(StandardOptions.Option.MAX_FRAMERATE.cast())
                        .setName(TextComponent.translatable("options.framerateLimit"))
                        .setTooltip(TextComponent.translatable("sodium.options.fps_limit.tooltip"))
                        .setControl(option -> new SliderControl(option, 10, 260, 10, ControlValueFormatter.fpsLimit()))
                        .setBinding((opts, value) -> {
                            opts.framerateLimit/*? if >=1.19 {*/().set/*?} else {*//*=*//*?}*/(value);
                            Minecraft.getInstance().getWindow().setFramerateLimit(value);
                        }, opts -> opts.framerateLimit/*? if >=1.19 {*/().get()/*?}*/)
                        .build())
                .build());

        groups.add(OptionGroup.createBuilder()
                .setId(StandardOptions.Group.INDICATORS)
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setId(StandardOptions.Option.VIEW_BOBBING.cast())
                        .setName(TextComponent.translatable("options.viewBobbing"))
                        .setTooltip(TextComponent.translatable("sodium.options.view_bobbing.tooltip"))
                        .setControl(TickBoxControl::new)
                        //? if >=1.19 {
                        .setBinding(new VanillaBooleanOptionBinding(Minecraft.getInstance().options.bobView()))
                        //?} else
                        /*.setBinding(new VanillaBooleanOptionBinding(Option.VIEW_BOBBING))*/
                        .build())
                .add(OptionImpl.createBuilder(AttackIndicatorStatus.class, vanillaOpts)
                        .setId(StandardOptions.Option.ATTACK_INDICATOR.cast())
                        .setName(TextComponent.translatable("options.attackIndicator"))
                        .setTooltip(TextComponent.translatable("sodium.options.attack_indicator.tooltip"))
                        .setControl(opts -> new CyclingControl<>(opts, AttackIndicatorStatus.class, new TextComponent[] { TextComponent.translatable("options.off"), TextComponent.translatable("options.attack.crosshair"), TextComponent.translatable("options.attack.hotbar") }))
                        .setBinding((opts, value) -> opts.attackIndicator/*? if >=1.19 {*/().set/*?} else {*//*=*//*?}*/(value), (opts) -> opts.attackIndicator/*? if >=1.19 {*/().get()/*?}*/)
                        .build())
                //? if >=1.18 {
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setId(StandardOptions.Option.AUTOSAVE_INDICATOR.cast())
                        .setName(TextComponent.translatable("options.autosaveIndicator"))
                        .setTooltip(TextComponent.translatable("sodium.options.autosave_indicator.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.showAutosaveIndicator/*? if >=1.19 {*/().set/*?} else {*//*=*//*?}*/(value), opts -> opts.showAutosaveIndicator/*? if >=1.19 {*/().get()/*?}*/)
                        .build())
                //?}
                .build());

        return new OptionPage(StandardOptions.Pages.GENERAL, TextComponent.translatable("stat.generalButton"), ImmutableList.copyOf(groups));
    }

    public static OptionPage quality() {
        var sodiumOpts = Celeritas.options();

        List<OptionGroup> groups = new ArrayList<>();

        groups.add(OptionGroup.createBuilder()
                .setId(StandardOptions.Group.GRAPHICS)
                .add(OptionImpl.createBuilder(/*? if >=1.16 {*/ GraphicsStatus.class /*?} else {*/ /*boolean.class *//*?}*/, vanillaOpts)
                        .setId(StandardOptions.Option.GRAPHICS_MODE.cast())
                        .setName(TextComponent.translatable("options.graphics"))
                        .setTooltip(TextComponent.translatable("sodium.options.graphics_quality.tooltip"))
                        //? if >=1.16 {
                        .setControl(option -> new CyclingControl<>(option, GraphicsStatus.class, new TextComponent[] { TextComponent.translatable("options.graphics.fast"), TextComponent.translatable("options.graphics.fancy"), TextComponent.translatable("options.graphics.fabulous").withStyle(TextFormattingStyle.ITALIC) }))
                        .setBinding(
                                (opts, value) -> opts.graphicsMode/*? if >=1.19 {*/().set/*?} else {*//*=*//*?}*/(value),
                                opts -> opts.graphicsMode/*? if >=1.19 {*/().get()/*?}*/)
                        //?} else {
                        /*.setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.fancyGraphics = value, opts -> opts.fancyGraphics)
                        *///?}
                        .setImpact(OptionImpact.HIGH)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .build());

        groups.add(OptionGroup.createBuilder()
                .setId(StandardOptions.Group.DETAILS)
                .add(OptionImpl.createBuilder(CloudStatus.class, vanillaOpts)
                        .setId(StandardOptions.Option.CLOUDS.cast())
                        .setName(TextComponent.translatable("options.renderClouds"))
                        .setTooltip(TextComponent.translatable("sodium.options.clouds_quality.tooltip"))
                        .setControl(option -> new CyclingControl<>(option, CloudStatus.class, new TextComponent[] { TextComponent.translatable("options.off"), TextComponent.translatable("options.graphics.fast"), TextComponent.translatable("options.graphics.fancy") }))
                        .setBinding((opts, value) -> {
                            //? if >=1.19 {
                            opts.cloudStatus().set(value);
                            //?} else {
                            /*opts.renderClouds = value;
                            *///?}

                            //? if >=1.16 {
                            if (Minecraft.useShaderTransparency()) {
                                RenderTarget framebuffer = Minecraft.getInstance().levelRenderer.getCloudsTarget();
                                if (framebuffer != null) {
                                    framebuffer.clear(Minecraft.ON_OSX);
                                }
                            }
                            //?}
                        }, opts -> {
                            //? if >=1.19 {
                            return opts.cloudStatus().get();
                            //?} else {
                            /*return opts.renderClouds;
                            *///?}
                        })
                        .setImpact(OptionImpact.LOW)
                        .build())
                .add(OptionImpl.createBuilder(SodiumGameOptions.GraphicsQuality.class, sodiumOpts)
                        .setId(StandardOptions.Option.WEATHER.cast())
                        .setName(TextComponent.translatable("soundCategory.weather"))
                        .setTooltip(TextComponent.translatable("sodium.options.weather_quality.tooltip"))
                        .setControl(option -> new CyclingControl<>(option, SodiumGameOptions.GraphicsQuality.class))
                        .setBinding((opts, value) -> opts.quality.weatherQuality = value, opts -> opts.quality.weatherQuality)
                        .setImpact(OptionImpact.MEDIUM)
                        .build())
                .add(OptionImpl.createBuilder(SodiumGameOptions.GraphicsQuality.class, sodiumOpts)
                        .setId(StandardOptions.Option.LEAVES.cast())
                        .setName(TextComponent.translatable("sodium.options.leaves_quality.name"))
                        .setTooltip(TextComponent.translatable("sodium.options.leaves_quality.tooltip"))
                        .setControl(option -> new CyclingControl<>(option, SodiumGameOptions.GraphicsQuality.class))
                        .setBinding((opts, value) -> opts.quality.leavesQuality = value, opts -> opts.quality.leavesQuality)
                        .setImpact(OptionImpact.MEDIUM)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .add(OptionImpl.createBuilder(ParticleStatus.class, vanillaOpts)
                        .setId(StandardOptions.Option.PARTICLES.cast())
                        .setName(TextComponent.translatable("options.particles"))
                        .setTooltip(TextComponent.translatable("sodium.options.particle_quality.tooltip"))
                        .setControl(option -> new CyclingControl<>(option, ParticleStatus.class, new TextComponent[] { TextComponent.translatable("options.particles.all"), TextComponent.translatable("options.particles.decreased"), TextComponent.translatable("options.particles.minimal") }))
                        .setBinding((opts, value) -> opts.particles/*? if >=1.19 {*/().set/*?} else {*//*=*//*?}*/(value), (opts) -> opts.particles/*? if >=1.19 {*/().get()/*?}*/)
                        .setImpact(OptionImpact.MEDIUM)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setId(StandardOptions.Option.SMOOTH_LIGHT.cast())
                        .setName(TextComponent.translatable("options.ao"))
                        .setTooltip(TextComponent.translatable("sodium.options.smooth_lighting.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.ambientOcclusion/*? if >=1.19 {*/().set/*?} else {*//*=*//*?}*/(/*? if <1.20 {*/  /*value ? AmbientOcclusionStatus.MAX : AmbientOcclusionStatus.OFF *//*?} else {*/ value /*?}*/), opts -> opts.ambientOcclusion/*? if >=1.19 {*/().get()/*?}*/ /*? if <1.20 {*/ /*!= AmbientOcclusionStatus.OFF *//*?}*/)
                        .setImpact(OptionImpact.LOW)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setId(StandardOptions.Option.BIOME_BLEND.cast())
                        .setName(TextComponent.translatable("options.biomeBlendRadius"))
                        .setTooltip(TextComponent.translatable("sodium.options.biome_blend.tooltip"))
                        .setControl(option -> new SliderControl(option, 0, 7, 1, ControlValueFormatter.biomeBlend()))
                        .setBinding((opts, value) -> opts.biomeBlendRadius/*? if >=1.19 {*/().set/*?} else {*//*=*//*?}*/(value), opts -> opts.biomeBlendRadius/*? if >=1.19 {*/().get()/*?}*/)
                        .setImpact(OptionImpact.LOW)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                //? if >=1.16 {
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setId(StandardOptions.Option.ENTITY_DISTANCE.cast())
                        .setName(TextComponent.translatable("options.entityDistanceScaling"))
                        .setTooltip(TextComponent.translatable("sodium.options.entity_distance.tooltip"))
                        .setControl(option -> new SliderControl(option, 50, 500, 25, ControlValueFormatter.percentage()))
                        .setBinding((opts, value) -> {
                            //? if >=1.19 {
                            opts.entityDistanceScaling().set(value / 100.0);
                            //?} else
                            /*opts.entityDistanceScaling = value / 100.0f;*/
                        }, opts -> Math.round(opts.entityDistanceScaling/*? if >=1.19 {*/().get().floatValue()/*?}*/ * 100.0F))
                        .setImpact(OptionImpact.MEDIUM)
                        .build()
                )
                //?}
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setId(StandardOptions.Option.ENTITY_SHADOWS.cast())
                        .setName(TextComponent.translatable("options.entityShadows"))
                        .setTooltip(TextComponent.translatable("sodium.options.entity_shadows.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.entityShadows/*? if >=1.19 {*/().set/*?} else {*//*=*//*?}*/(value), opts -> opts.entityShadows/*? if >=1.19 {*/().get()/*?}*/)
                        .setImpact(OptionImpact.LOW)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setId(StandardOptions.Option.VIGNETTE.cast())
                        .setName(TextComponent.translatable("sodium.options.vignette.name"))
                        .setTooltip(TextComponent.translatable("sodium.options.vignette.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.quality.enableVignette = value, opts -> opts.quality.enableVignette)
                        .setImpact(OptionImpact.LOW)
                        .build())
                .build());


        groups.add(OptionGroup.createBuilder()
                .setId(StandardOptions.Group.MIPMAPS)
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setId(StandardOptions.Option.MIPMAP_LEVEL.cast())
                        .setName(TextComponent.translatable("options.mipmapLevels"))
                        .setTooltip(TextComponent.translatable("sodium.options.mipmap_levels.tooltip"))
                        .setControl(option -> new SliderControl(option, 0, 4, 1, ControlValueFormatter.multiplier()))
                        .setBinding((opts, value) -> opts.mipmapLevels/*? if >=1.19 {*/().set/*?} else {*//*=*//*?}*/(value), opts -> opts.mipmapLevels/*? if >=1.19 {*/().get()/*?}*/)
                        .setImpact(OptionImpact.MEDIUM)
                        .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD)
                        .build())
                .build());

        groups.add(CommonOptionPages.sortingGroup(sodiumOpts));

        groups.add(OptionGroup.createBuilder()
                .setId(StandardOptions.Group.LIGHTING)
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setId(StandardOptions.Option.USE_QUAD_NORMALS_FOR_LIGHTING.cast())
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.LOW)
                        .setBinding((opts, value) -> opts.quality.useQuadNormalsForShading = value, opts -> opts.quality.useQuadNormalsForShading)
                        //? if forge
                        .setEnabled(!ForgeConfig.CLIENT.experimentalForgeLightPipelineEnabled.get())
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .build());

        return new OptionPage(StandardOptions.Pages.QUALITY, TextComponent.translatable("sodium.options.pages.quality"), ImmutableList.copyOf(groups));
    }



    private static boolean supportsNoErrorContext() {
        //? if >=1.18 {
        GLCapabilities capabilities = GL.getCapabilities();
        return (capabilities.OpenGL46 || capabilities.GL_KHR_no_error)
                && !Workarounds.isWorkaroundEnabled(Workarounds.Reference.NO_ERROR_CONTEXT_UNSUPPORTED);
        //?} else
        /*return false;*/
    }

    public static OptionPage advanced() {
        var sodiumOpts = Celeritas.options();

        List<OptionGroup> groups = new ArrayList<>();

        groups.add(OptionGroup.createBuilder()
                .setId(StandardOptions.Group.CPU_SAVING)
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setId(StandardOptions.Option.PERSISTENT_MAPPING.cast())
                        .setName(TextComponent.translatable("sodium.options.use_persistent_mapping.name"))
                        .setTooltip(TextComponent.translatable("sodium.options.use_persistent_mapping.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.MEDIUM)
                        .setEnabled(MappedStagingBuffer.isSupported(RenderDevice.INSTANCE))
                        .setBinding((opts, value) -> opts.advanced.useAdvancedStagingBuffers = value, opts -> opts.advanced.useAdvancedStagingBuffers)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build()
                )
                .add(OptionImpl.createBuilder(int.class, sodiumOpts)
                        .setId(StandardOptions.Option.CPU_FRAMES_AHEAD.cast())
                        .setName(TextComponent.translatable("sodium.options.cpu_render_ahead_limit.name"))
                        .setTooltip(TextComponent.translatable("sodium.options.cpu_render_ahead_limit.tooltip"))
                        .setControl(opt -> new SliderControl(opt, 0, 9, 1, ControlValueFormatter.translateVariable("sodium.options.cpu_render_ahead_limit.value")))
                        .setBinding((opts, value) -> opts.advanced.cpuRenderAheadLimit = value, opts -> opts.advanced.cpuRenderAheadLimit)
                        .build()
                )
                .build());

        return new OptionPage(StandardOptions.Pages.ADVANCED, TextComponent.translatable("sodium.options.pages.advanced"), ImmutableList.copyOf(groups));
    }

    public static OptionStorage<Options> getVanillaOpts() {
        return vanillaOpts;
    }
}
