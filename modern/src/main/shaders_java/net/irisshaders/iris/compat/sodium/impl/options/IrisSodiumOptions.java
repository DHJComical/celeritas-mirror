package net.irisshaders.iris.compat.sodium.impl.options;

//? if settings_gui {

import com.google.common.collect.ImmutableList;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.config.IrisConfig;
import net.irisshaders.iris.gui.option.IrisVideoSettings;
import net.irisshaders.iris.pathways.colorspace.ColorSpace;
import net.minecraft.client.Options;
import org.taumc.celeritas.api.OptionGUIConstructionEvent;
import org.taumc.celeritas.api.OptionGroupConstructionEvent;
import org.taumc.celeritas.api.options.control.ControlValueFormatter;
import org.taumc.celeritas.api.options.control.CyclingControl;
import org.taumc.celeritas.api.options.control.SliderControl;
import org.taumc.celeritas.api.options.control.TickBoxControl;
import org.embeddedt.embeddium.impl.gui.modern.SodiumGameOptionPages;
import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.taumc.celeritas.api.options.structure.*;
import org.taumc.celeritas.shaders.CeleritasShaders;

import java.io.IOException;
import java.util.Set;

public class IrisSodiumOptions {
    private static final OptionStorage<IrisConfig> irisOpts = new OptionStorage<IrisConfig>() {
        @Override
        public IrisConfig getData() {
            return Iris.getIrisConfig();
        }

        @Override
        public void save(Set<OptionFlag> flags) {
            try {
                getData().save();

                if (flags.contains(OptionFlag.REQUIRES_SHADER_PIPELINE_RELOAD)) {
                    Iris.reload();
                }
            } catch (IOException e) {
                CeleritasShaders.logger().error("Error saving config", e);
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
            ev.addPage(new OptionPage(StandardOptions.Pages.SHADERS, TextComponent.literal("Shaders"), ImmutableList.of(
                    OptionGroup.createBuilder()
                            .add(OptionImpl.createBuilder(boolean.class, irisOpts)
                                    .setName(TextComponent.literal("Enable Texture Material Fallback"))
                                    .setTooltip(TextComponent.literal("Uses textures to guess block.properties IDs if they are not set."))
                                    .setControl(TickBoxControl::new)
                                    .setBinding(IrisConfig::setEnableTextureMaterialFallback, IrisConfig::isEnableTextureMaterialFallback)
                                    .setFlags(OptionFlag.REQUIRES_SHADER_PIPELINE_RELOAD)
                                    .build())
                            .add(OptionImpl.createBuilder(boolean.class, irisOpts)
                                    .setName(TextComponent.literal("Block Modded Core Shaders"))
                                    .setTooltip(TextComponent.literal("Prevents modded core shaders from being active with the Iris pipeline."))
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
			.setName(TextComponent.translatable("options.iris.shadowDistance"))
			.setTooltip(TextComponent.translatable("options.iris.shadowDistance.sodium_tooltip"))
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
	}

	public static OptionImpl<Options, ColorSpace> createColorSpaceButton(OptionStorage<Options> vanillaOpts) {
		OptionImpl<Options, ColorSpace> colorSpace = OptionImpl.createBuilder(ColorSpace.class, vanillaOpts)
			.setName(TextComponent.translatable("options.iris.colorSpace"))
			.setTooltip(TextComponent.translatable("options.iris.colorSpace.sodium_tooltip"))
			.setControl(option -> new CyclingControl<>(option, ColorSpace.class,
				new TextComponent[]{TextComponent.literal("sRGB"), TextComponent.literal("DCI_P3"), TextComponent.literal("Display P3"), TextComponent.literal("REC2020"), TextComponent.literal("Adobe RGB")}))
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
			return v == 0 ? TextComponent.literal(disabled) : (TextComponent.translatable(key, v));
		};
	}
}
//?}