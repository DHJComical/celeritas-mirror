package org.embeddedt.embeddium.impl.gui.modern;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.GraphicsPreset;
import net.minecraft.client.InactivityFpsLimit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.server.level.ParticleStatus;
import org.embeddedt.embeddium.impl.Celeritas;
import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.embeddedt.embeddium.impl.gui.framework.TextFormattingStyle;
import org.embeddedt.embeddium.impl.gui.options.CommonOptionPages;
import org.taumc.celeritas.api.OptionGroupConstructionEvent;
import org.taumc.celeritas.api.options.OptionIdentifier;
import org.taumc.celeritas.api.options.control.ControlValueFormatter;
import org.taumc.celeritas.api.options.control.CyclingControl;
import org.taumc.celeritas.api.options.control.SliderControl;
import org.taumc.celeritas.api.options.control.TickBoxControl;
import org.taumc.celeritas.api.options.structure.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SodiumGameOptionPages {
    private static final MinecraftOptionsStorage vanillaOpts = new MinecraftOptionsStorage();

    /**
     * Shared options that nothing reads on this version.
     */
    private static final Set<OptionIdentifier<?>> UNSUPPORTED_OPTIONS = Set.of(
            StandardOptions.Option.CHUNK_FADE_IN_DURATION,
            StandardOptions.Option.ANIMATE_VISIBLE_TEXTURES,
            StandardOptions.Option.RENDER_PASS_CONSOLIDATION,
            StandardOptions.Option.USE_FASTER_CLOUDS
    );

    static {
        OptionGroupConstructionEvent.BUS.addListener(event -> event.getOptions().removeIf(option -> UNSUPPORTED_OPTIONS.contains(option.getId())));
    }

    private static int computeMaxRangeForRenderDistance(@SuppressWarnings("SameParameterValue") int injectedRenderDistance) {
        if(vanillaOpts.getData().renderDistance().values() instanceof OptionInstance.IntRange range) {
            injectedRenderDistance = Math.max(injectedRenderDistance, range.maxInclusive());
        }
        return injectedRenderDistance;
    }

    private static int getMaxGuiScale() {
        if (vanillaOpts.getData().guiScale().values() instanceof OptionInstance.ClampingLazyMaxIntRange range) {
            return range.maxInclusive();
        }
        return 4;
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
                        .setBinding((options, value) -> options.renderDistance().set(value), options -> options.renderDistance().get())
                        .setImpact(OptionImpact.HIGH)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setId(StandardOptions.Option.SIMULATION_DISTANCE.cast())
                        .setName(TextComponent.translatable("options.simulationDistance"))
                        .setTooltip(TextComponent.translatable("sodium.options.simulation_distance.tooltip"))
                        .setControl(option -> new SliderControl(option, 5, 32, 1, ControlValueFormatter.translateVariable("options.chunks")))
                        .setBinding((options, value) -> options.simulationDistance().set(value), options -> options.simulationDistance().get())
                        .setImpact(OptionImpact.HIGH)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setId(StandardOptions.Option.BRIGHTNESS.cast())
                        .setName(TextComponent.translatable("options.gamma"))
                        .setTooltip(TextComponent.translatable("sodium.options.brightness.tooltip"))
                        .setControl(opt -> new SliderControl(opt, 0, 100, 1, ControlValueFormatter.brightness()))
                        .setBinding((opts, value) -> opts.gamma().set(value * 0.01D), (opts) -> (int) (opts.gamma().get() / 0.01D))
                        .build())
                .build());

        groups.add(OptionGroup.createBuilder()
                .setId(StandardOptions.Group.WINDOW)
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setId(StandardOptions.Option.GUI_SCALE.cast())
                        .setName(TextComponent.translatable("options.guiScale"))
                        .setTooltip(TextComponent.translatable("sodium.options.gui_scale.tooltip"))
                        .setControl(option -> new SliderControl(option, 0, getMaxGuiScale(), 1, ControlValueFormatter.guiScale()))
                        .setBinding((opts, value) -> opts.guiScale().set(value), opts -> opts.guiScale().get())
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setId(StandardOptions.Option.FULLSCREEN.cast())
                        .setName(TextComponent.translatable("options.fullscreen"))
                        .setTooltip(TextComponent.translatable("sodium.options.fullscreen.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding(new VanillaBooleanOptionBinding(Minecraft.getInstance().options.fullscreen()))
                        .build())
                .add(FullscreenResolutionHelper.createFullScreenResolutionOption())
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setId(StandardOptions.Option.EXCLUSIVE_FULLSCREEN.cast())
                        .setName(TextComponent.translatable("options.exclusiveFullscreen"))
                        .setTooltip(TextComponent.translatable("options.exclusiveFullscreen.warningTooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding(new VanillaBooleanOptionBinding(Minecraft.getInstance().options.exclusiveFullscreen()))
                        .setFlags(OptionFlag.REQUIRES_GAME_RESTART)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setId(StandardOptions.Option.VSYNC.cast())
                        .setName(TextComponent.translatable("options.vsync"))
                        .setTooltip(TextComponent.translatable("sodium.options.v_sync.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding(new VanillaBooleanOptionBinding(Minecraft.getInstance().options.enableVsync()))
                        .setImpact(OptionImpact.VARIES)
                        .build())
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setId(StandardOptions.Option.MAX_FRAMERATE.cast())
                        .setName(TextComponent.translatable("options.framerateLimit"))
                        .setTooltip(TextComponent.translatable("sodium.options.fps_limit.tooltip"))
                        .setControl(option -> new SliderControl(option, 10, 260, 10, ControlValueFormatter.fpsLimit()))
                        .setBinding((opts, value) -> opts.framerateLimit().set(value), opts -> opts.framerateLimit().get())
                        .build())
                .add(OptionImpl.createBuilder(InactivityFpsLimit.class, vanillaOpts)
                        .setId(StandardOptions.Option.INACTIVITY_FPS_LIMIT.cast())
                        .setName(TextComponent.translatable("options.inactivityFpsLimit"))
                        .setTooltip(TextComponent.translatable("options.inactivityFpsLimit.afk.tooltip"))
                        .setControl(option -> new CyclingControl<>(option, InactivityFpsLimit.class, new TextComponent[] { TextComponent.translatable("options.inactivityFpsLimit.minimized"), TextComponent.translatable("options.inactivityFpsLimit.afk") }))
                        .setBinding((opts, value) -> opts.inactivityFpsLimit().set(value), opts -> opts.inactivityFpsLimit().get())
                        .build())
                .build());

        groups.add(OptionGroup.createBuilder()
                .setId(StandardOptions.Group.INDICATORS)
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setId(StandardOptions.Option.VIEW_BOBBING.cast())
                        .setName(TextComponent.translatable("options.viewBobbing"))
                        .setTooltip(TextComponent.translatable("sodium.options.view_bobbing.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding(new VanillaBooleanOptionBinding(Minecraft.getInstance().options.bobView()))
                        .build())
                .add(OptionImpl.createBuilder(AttackIndicatorStatus.class, vanillaOpts)
                        .setId(StandardOptions.Option.ATTACK_INDICATOR.cast())
                        .setName(TextComponent.translatable("options.attackIndicator"))
                        .setTooltip(TextComponent.translatable("sodium.options.attack_indicator.tooltip"))
                        .setControl(opts -> new CyclingControl<>(opts, AttackIndicatorStatus.class, new TextComponent[] { TextComponent.translatable("options.off"), TextComponent.translatable("options.attack.crosshair"), TextComponent.translatable("options.attack.hotbar") }))
                        .setBinding((opts, value) -> opts.attackIndicator().set(value), (opts) -> opts.attackIndicator().get())
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setId(StandardOptions.Option.AUTOSAVE_INDICATOR.cast())
                        .setName(TextComponent.translatable("options.autosaveIndicator"))
                        .setTooltip(TextComponent.translatable("sodium.options.autosave_indicator.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding(new VanillaBooleanOptionBinding(Minecraft.getInstance().options.showAutosaveIndicator()))
                        .build())
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setId(StandardOptions.Option.MENU_BACKGROUND_BLUR.cast())
                        .setName(TextComponent.translatable("options.accessibility.menu_background_blurriness"))
                        .setTooltip(TextComponent.translatable("options.accessibility.menu_background_blurriness.tooltip"))
                        .setControl(option -> new SliderControl(option, 0, 10, 1, ControlValueFormatter.number()))
                        .setBinding((opts, value) -> opts.menuBackgroundBlurriness().set(value), opts -> opts.menuBackgroundBlurriness().get())
                        .build())
                .build());

        return new OptionPage(StandardOptions.Pages.GENERAL, TextComponent.translatable("stat.generalButton"), ImmutableList.copyOf(groups));
    }

    public static OptionPage quality() {
        var sodiumOpts = Celeritas.options();

        List<OptionGroup> groups = new ArrayList<>();

        // Options whose vanilla setters already reload the level renderer carry no reload flag of their own.
        groups.add(OptionGroup.createBuilder()
                .setId(StandardOptions.Group.GRAPHICS)
                .add(OptionImpl.createBuilder(GraphicsPreset.class, vanillaOpts)
                        .setId(StandardOptions.Option.GRAPHICS_MODE.cast())
                        .setName(TextComponent.translatable("options.graphics.preset"))
                        .setTooltip(TextComponent.translatable("options.graphics.preset.tooltip"))
                        .setControl(option -> new CyclingControl<>(option, GraphicsPreset.class, new TextComponent[] { TextComponent.translatable("options.graphics.fast"), TextComponent.translatable("options.graphics.fancy"), TextComponent.translatable("options.graphics.fabulous").withStyle(TextFormattingStyle.ITALIC), TextComponent.translatable("options.graphics.custom") }))
                        .setBinding((opts, value) -> opts.graphicsPreset().set(value), opts -> opts.graphicsPreset().get())
                        .setImpact(OptionImpact.HIGH)
                        .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setId(StandardOptions.Option.IMPROVED_TRANSPARENCY.cast())
                        .setName(TextComponent.translatable("options.improvedTransparency"))
                        .setTooltip(TextComponent.translatable("options.improvedTransparency.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> {
                            // Vanilla defers this to the unsupported GPU warning dialog on its own screen, which this screen doesn't show.
                            Minecraft.getInstance().getGpuWarnlistManager().dismissWarning();
                            opts.improvedTransparency().set(value);
                        }, opts -> opts.improvedTransparency().get())
                        .setImpact(OptionImpact.HIGH)
                        .build())
                .build());

        groups.add(OptionGroup.createBuilder()
                .setId(StandardOptions.Group.DETAILS)
                .add(OptionImpl.createBuilder(CloudStatus.class, vanillaOpts)
                        .setId(StandardOptions.Option.CLOUDS.cast())
                        .setName(TextComponent.translatable("options.renderClouds"))
                        .setTooltip(TextComponent.translatable("sodium.options.clouds_quality.tooltip"))
                        .setControl(option -> new CyclingControl<>(option, CloudStatus.class, new TextComponent[] { TextComponent.translatable("options.off"), TextComponent.translatable("options.clouds.fast"), TextComponent.translatable("options.clouds.fancy") }))
                        .setBinding((opts, value) -> opts.cloudStatus().set(value), opts -> opts.cloudStatus().get())
                        .setImpact(OptionImpact.LOW)
                        .build())
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setId(StandardOptions.Option.CLOUD_RANGE.cast())
                        .setName(TextComponent.translatable("options.renderCloudsDistance"))
                        .setControl(option -> new SliderControl(option, 2, 128, 1, ControlValueFormatter.translateVariable("options.chunks")))
                        .setBinding((opts, value) -> opts.cloudRange().set(value), opts -> opts.cloudRange().get())
                        .setImpact(OptionImpact.LOW)
                        .build())
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setId(StandardOptions.Option.WEATHER.cast())
                        .setName(TextComponent.translatable("options.weatherRadius"))
                        .setTooltip(TextComponent.translatable("options.weatherRadius.tooltip"))
                        .setControl(option -> new SliderControl(option, 3, 10, 1, ControlValueFormatter.translateVariable("options.blocks")))
                        .setBinding((opts, value) -> opts.weatherRadius().set(value), opts -> opts.weatherRadius().get())
                        .setImpact(OptionImpact.MEDIUM)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setId(StandardOptions.Option.LEAVES.cast())
                        .setName(TextComponent.translatable("options.cutoutLeaves"))
                        .setTooltip(TextComponent.translatable("options.cutoutLeaves.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding(new VanillaBooleanOptionBinding(Minecraft.getInstance().options.cutoutLeaves()))
                        .setImpact(OptionImpact.MEDIUM)
                        .build())
                .add(OptionImpl.createBuilder(ParticleStatus.class, vanillaOpts)
                        .setId(StandardOptions.Option.PARTICLES.cast())
                        .setName(TextComponent.translatable("options.particles"))
                        .setTooltip(TextComponent.translatable("sodium.options.particle_quality.tooltip"))
                        .setControl(option -> new CyclingControl<>(option, ParticleStatus.class, new TextComponent[] { TextComponent.translatable("options.particles.all"), TextComponent.translatable("options.particles.decreased"), TextComponent.translatable("options.particles.minimal") }))
                        .setBinding((opts, value) -> opts.particles().set(value), (opts) -> opts.particles().get())
                        .setImpact(OptionImpact.MEDIUM)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setId(StandardOptions.Option.SMOOTH_LIGHT.cast())
                        .setName(TextComponent.translatable("options.ao"))
                        .setTooltip(TextComponent.translatable("sodium.options.smooth_lighting.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding(new VanillaBooleanOptionBinding(Minecraft.getInstance().options.ambientOcclusion()))
                        .setImpact(OptionImpact.LOW)
                        .build())
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setId(StandardOptions.Option.BIOME_BLEND.cast())
                        .setName(TextComponent.translatable("options.biomeBlendRadius"))
                        .setTooltip(TextComponent.translatable("sodium.options.biome_blend.tooltip"))
                        .setControl(option -> new SliderControl(option, 0, 7, 1, ControlValueFormatter.biomeBlend()))
                        .setBinding((opts, value) -> opts.biomeBlendRadius().set(value), opts -> opts.biomeBlendRadius().get())
                        .setImpact(OptionImpact.LOW)
                        .build())
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setId(StandardOptions.Option.ENTITY_DISTANCE.cast())
                        .setName(TextComponent.translatable("options.entityDistanceScaling"))
                        .setTooltip(TextComponent.translatable("sodium.options.entity_distance.tooltip"))
                        .setControl(option -> new SliderControl(option, 50, 500, 25, ControlValueFormatter.percentage()))
                        .setBinding((opts, value) -> opts.entityDistanceScaling().set(value / 100.0), opts -> (int) Math.round(opts.entityDistanceScaling().get() * 100.0))
                        .setImpact(OptionImpact.MEDIUM)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setId(StandardOptions.Option.ENTITY_SHADOWS.cast())
                        .setName(TextComponent.translatable("options.entityShadows"))
                        .setTooltip(TextComponent.translatable("sodium.options.entity_shadows.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding(new VanillaBooleanOptionBinding(Minecraft.getInstance().options.entityShadows()))
                        .setImpact(OptionImpact.LOW)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setId(StandardOptions.Option.VIGNETTE.cast())
                        .setName(TextComponent.translatable("options.vignette"))
                        .setTooltip(TextComponent.translatable("options.vignette.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding(new VanillaBooleanOptionBinding(Minecraft.getInstance().options.vignette()))
                        .setImpact(OptionImpact.LOW)
                        .build())
                // In twentieths of a second, as vanilla's slider steps.
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setId(StandardOptions.Option.CHUNK_FADE.cast())
                        .setName(TextComponent.translatable("options.chunkFade"))
                        .setTooltip(TextComponent.translatable("options.chunkFade.tooltip"))
                        .setControl(option -> new SliderControl(option, 0, 40, 1, value -> value == 0
                                ? TextComponent.translatable("options.chunkFade.none")
                                : TextComponent.translatable("options.chunkFade.seconds", String.format(Locale.ROOT, "%.2f", value / 20.0))))
                        .setBinding((opts, value) -> opts.chunkSectionFadeInTime().set(value / 20.0), opts -> (int) Math.round(opts.chunkSectionFadeInTime().get() * 20.0))
                        .setImpact(OptionImpact.LOW)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .build());

        groups.add(OptionGroup.createBuilder()
                .setId(StandardOptions.Group.MIPMAPS)
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setId(StandardOptions.Option.MIPMAP_LEVEL.cast())
                        .setName(TextComponent.translatable("options.mipmapLevels"))
                        .setTooltip(TextComponent.translatable("sodium.options.mipmap_levels.tooltip"))
                        .setControl(option -> new SliderControl(option, 0, 4, 1, ControlValueFormatter.multiplier()))
                        .setBinding((opts, value) -> opts.mipmapLevels().set(value), opts -> opts.mipmapLevels().get())
                        .setImpact(OptionImpact.MEDIUM)
                        .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD)
                        .build())
                .add(OptionImpl.createBuilder(TextureFilteringMethod.class, vanillaOpts)
                        .setId(StandardOptions.Option.TEXTURE_FILTERING.cast())
                        .setName(TextComponent.translatable("options.textureFiltering"))
                        .setTooltip(TextComponent.translatable("options.textureFiltering.anisotropic.tooltip"))
                        .setControl(option -> new CyclingControl<>(option, TextureFilteringMethod.class, new TextComponent[] { TextComponent.translatable("options.textureFiltering.none"), TextComponent.translatable("options.textureFiltering.rgss"), TextComponent.translatable("options.textureFiltering.anisotropic") }))
                        .setBinding((opts, value) -> opts.textureFiltering().set(value), opts -> opts.textureFiltering().get())
                        .setImpact(OptionImpact.MEDIUM)
                        .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD)
                        .build())
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setId(StandardOptions.Option.MAX_ANISOTROPY.cast())
                        .setName(TextComponent.translatable("options.maxAnisotropy"))
                        .setTooltip(TextComponent.translatable("options.maxAnisotropy.tooltip"))
                        .setControl(option -> new SliderControl(option, 1, 3, 1, value -> TextComponent.translatable("options.multiplier", Integer.toString(1 << value))))
                        .setBinding((opts, value) -> opts.maxAnisotropyBit().set(value), opts -> opts.maxAnisotropyBit().get())
                        .setEnabledPredicate(() -> vanillaOpts.getData().textureFiltering().get() == TextureFilteringMethod.ANISOTROPIC)
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
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .build());

        return new OptionPage(StandardOptions.Pages.QUALITY, TextComponent.translatable("sodium.options.pages.quality"), ImmutableList.copyOf(groups));
    }

    public static OptionStorage<Options> getVanillaOpts() {
        return vanillaOpts;
    }
}
