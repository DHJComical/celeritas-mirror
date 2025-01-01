package net.irisshaders.iris.compat.sodium.impl.options;

import net.irisshaders.iris.Iris;
import net.irisshaders.iris.gui.option.IrisVideoSettings;
import net.irisshaders.iris.pathways.colorspace.ColorSpace;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
import org.embeddedt.embeddium.api.OptionGroupConstructionEvent;
import org.embeddedt.embeddium.api.options.control.ControlValueFormatter;
import org.embeddedt.embeddium.api.options.control.CyclingControl;
import org.embeddedt.embeddium.api.options.control.SliderControl;
import org.embeddedt.embeddium.api.options.storage.MinecraftOptionsStorage;
import org.embeddedt.embeddium.api.options.structure.*;
import org.embeddedt.embeddium.impl.gui.SodiumGameOptionPages;

import java.io.IOException;

public class IrisSodiumOptions {
    public static void init() {
        OptionGroupConstructionEvent.BUS.addListener(ev -> {
            if(ev.getId().matches(StandardOptions.Group.RENDERING)) {
                ev.getOptions().add(1, createMaxShadowDistanceSlider(SodiumGameOptionPages.getVanillaOpts()));
            } else if(ev.getId().matches(StandardOptions.Group.GRAPHICS)) {
                ev.getOptions().add(createColorSpaceButton(SodiumGameOptionPages.getVanillaOpts()));
            }
        });
    }

	public static OptionImpl<Options, Integer> createMaxShadowDistanceSlider(OptionStorage<Options> vanillaOpts) {
		OptionImpl<Options, Integer> maxShadowDistanceSlider = OptionImpl.createBuilder(int.class, vanillaOpts)
			.setName(Component.translatable("options.iris.shadowDistance"))
			.setTooltip(Component.translatable("options.iris.shadowDistance.sodium_tooltip"))
			.setControl(option -> new SliderControl(option, 0, 32, 1, translateVariableOrDisabled("options.chunks", "Disabled")))
			.setBinding((options, value) -> {
					IrisVideoSettings.shadowDistance = value;
					try {
						Iris.getIrisConfig().save();
					} catch (IOException e) {
						e.printStackTrace();
					}
				},
				options -> IrisVideoSettings.getOverriddenShadowDistance(IrisVideoSettings.shadowDistance))
			.setImpact(OptionImpact.HIGH)
			.setEnabledPredicate(IrisVideoSettings::isShadowDistanceSliderEnabled)
			.build();

		return maxShadowDistanceSlider;
	}

	public static OptionImpl<Options, ColorSpace> createColorSpaceButton(OptionStorage<Options> vanillaOpts) {
		OptionImpl<Options, ColorSpace> colorSpace = OptionImpl.createBuilder(ColorSpace.class, vanillaOpts)
			.setName(Component.translatable("options.iris.colorSpace"))
			.setTooltip(Component.translatable("options.iris.colorSpace.sodium_tooltip"))
			.setControl(option -> new CyclingControl<>(option, ColorSpace.class,
				new Component[]{Component.literal("sRGB"), Component.literal("DCI_P3"), Component.literal("Display P3"), Component.literal("REC2020"), Component.literal("Adobe RGB")}))
			.setBinding((options, value) -> {
					IrisVideoSettings.colorSpace = value;
					try {
						Iris.getIrisConfig().save();
					} catch (IOException e) {
						e.printStackTrace();
					}
				},
				options -> IrisVideoSettings.colorSpace)
			.setImpact(OptionImpact.LOW)
			.setEnabled(true)
			.build();


		return colorSpace;
	}

	static ControlValueFormatter translateVariableOrDisabled(String key, String disabled) {
		return (v) -> {
			return v == 0 ? Component.literal(disabled) : (Component.translatable(key, v));
		};
	}

	public static OptionImpl<Options, SupportedGraphicsMode> createLimitedVideoSettingsButton(MinecraftOptionsStorage vanillaOpts) {
		return OptionImpl.createBuilder(SupportedGraphicsMode.class, vanillaOpts)
			.setName(Component.translatable("options.graphics"))
			// TODO: State that Fabulous Graphics is incompatible with Shader Packs in the tooltip
			.setTooltip(Component.translatable("sodium.options.graphics_quality.tooltip"))
			.setControl(option -> new CyclingControl<>(option, SupportedGraphicsMode.class,
				new Component[]{Component.translatable("options.graphics.fast"), Component.translatable("options.graphics.fancy")}))
			.setBinding(
				(opts, value) -> opts.graphicsMode().set(value.toVanilla()),
				opts -> SupportedGraphicsMode.fromVanilla(opts.graphicsMode().get()))
			.setImpact(OptionImpact.HIGH)
			.setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
			.build();
	}
}
