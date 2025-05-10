package net.irisshaders.iris.compat.sodium.impl.options;

import com.google.common.collect.ImmutableList;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.IrisCommon;
import net.irisshaders.iris.config.IrisConfig;
import net.irisshaders.iris.gui.option.IrisVideoSettings;
import net.irisshaders.iris.pathways.colorspace.ColorSpace;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
import org.embeddedt.embeddium.api.OptionGUIConstructionEvent;
import org.embeddedt.embeddium.api.OptionGroupConstructionEvent;
import org.embeddedt.embeddium.api.options.control.ControlValueFormatter;
import org.embeddedt.embeddium.api.options.control.CyclingControl;
import org.embeddedt.embeddium.api.options.control.SliderControl;
import org.embeddedt.embeddium.api.options.control.TickBoxControl;
import org.embeddedt.embeddium.api.options.structure.*;
import org.embeddedt.embeddium.impl.gui.SodiumGameOptionPages;

import java.io.IOException;
import java.util.Set;

import static net.irisshaders.iris.IrisLogging.IRIS_LOGGER;

public class IrisSodiumOptions {
    private static final OptionStorage<IrisConfig> irisOpts = new OptionStorage<IrisConfig>() {
        @Override
        public IrisConfig getData() {
            return IrisCommon.getIrisConfig();
        }

        @Override
        public void save(Set<OptionFlag> flags) {
            try {
                getData().save();

                if (flags.contains(OptionFlag.REQUIRES_SHADER_PIPELINE_RELOAD)) {
                    Iris.reload();
                }
            } catch (IOException e) {
                IRIS_LOGGER.error("Error saving config", e);
            }
        }
    };

    public static void init() {
        OptionGroupConstructionEvent.BUS.addListener(ev -> {
            if(ev.getId().matches(StandardOptions.Group.RENDERING)) {
                ev.getOptions().add(1, createMaxShadowDistanceSlider(SodiumGameOptionPages.getVanillaOpts()));
            } else if(ev.getId().matches(StandardOptions.Group.GRAPHICS)) {
                ev.getOptions().add(createColorSpaceButton(SodiumGameOptionPages.getVanillaOpts()));
            }
        });
        OptionGUIConstructionEvent.BUS.addListener(ev -> {
            ev.addPage(new OptionPage(StandardOptions.Pages.SHADERS, Component.literal("Shaders"), ImmutableList.of(
                    OptionGroup.createBuilder()
                            .add(OptionImpl.createBuilder(boolean.class, irisOpts)
                                    .setName(Component.literal("Enable Texture Material Fallback"))
                                    .setTooltip(Component.literal("Uses textures to guess block.properties IDs if they are not set."))
                                    .setControl(TickBoxControl::new)
                                    .setBinding(IrisConfig::setEnableTextureMaterialFallback, IrisConfig::isEnableTextureMaterialFallback)
                                    .setFlags(OptionFlag.REQUIRES_SHADER_PIPELINE_RELOAD)
                                    .build())
                            .add(OptionImpl.createBuilder(boolean.class, irisOpts)
                                    .setName(Component.literal("Block Modded Core Shaders"))
                                    .setTooltip(Component.literal("Prevents modded core shaders from being active with the Iris pipeline."))
                                    .setControl(TickBoxControl::new)
                                    .setBinding(IrisConfig::setBlockUnknownShaders, IrisConfig::isBlockUnknownShaders)
                                    .setFlags(OptionFlag.REQUIRES_SHADER_PIPELINE_RELOAD)
                                    .build())
                            .build()
            )));
        });
    }

	public static OptionImpl<IrisConfig, Integer> createMaxShadowDistanceSlider(OptionStorage<Options> vanillaOpts) {
		return OptionImpl.createBuilder(int.class, irisOpts)
			.setName(Component.translatable("options.iris.shadowDistance"))
			.setTooltip(Component.translatable("options.iris.shadowDistance.sodium_tooltip"))
			.setControl(option -> new SliderControl(option, 0, 32, 1, translateVariableOrDisabled("options.chunks", "Disabled")))
			.setBinding((options, value) -> {
					IrisVideoSettings.shadowDistance = value;
					try {
						IrisCommon.getIrisConfig().save();
					} catch (IOException e) {
						e.printStackTrace();
					}
				},
				options -> IrisVideoSettings.getOverriddenShadowDistance(IrisVideoSettings.shadowDistance))
			.setImpact(OptionImpact.HIGH)
			.setEnabledPredicate(IrisVideoSettings::isShadowDistanceSliderEnabled)
			.build();
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
						IrisCommon.getIrisConfig().save();
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
}
