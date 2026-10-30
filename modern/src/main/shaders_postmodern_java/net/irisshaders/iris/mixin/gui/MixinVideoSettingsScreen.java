package net.irisshaders.iris.mixin.gui;

import net.irisshaders.iris.Iris;
import net.irisshaders.iris.gui.option.IrisVideoSettings;
import net.irisshaders.iris.gui.screen.ShaderPackScreen;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.taumc.celeritas.shaders.CeleritasShaders;

import java.io.IOException;

/**
 * Adds the shader pack screen and the shadow distance to the end of the quality options.
 */
@Mixin(VideoSettingsScreen.class)
public abstract class MixinVideoSettingsScreen extends OptionsSubScreen {
	public MixinVideoSettingsScreen(Screen lastScreen, Options options, Component title) {
		super(lastScreen, options, title);
	}

	@Inject(method = "addOptions", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/OptionsList;addSmall([Lnet/minecraft/client/OptionInstance;)V", ordinal = 1, shift = At.Shift.AFTER))
	private void iris$addShaderOptions(CallbackInfo ci) {
		Button shaderPacks = Button.builder(Component.translatable("options.iris.shaderPackSelection"),
			button -> this.minecraft.setScreen(new ShaderPackScreen(this))).build();

		AbstractWidget shadowDistance = iris$createShadowDistanceOption().createButton(this.options);
		shadowDistance.active = IrisVideoSettings.isShadowDistanceSliderEnabled();

		this.list.addSmall(shaderPacks, shadowDistance);
	}

	@Unique
	private static OptionInstance<Integer> iris$createShadowDistanceOption() {
		boolean enabled = IrisVideoSettings.isShadowDistanceSliderEnabled();
		Tooltip tooltip = Tooltip.create(Component.translatable(enabled ? "options.iris.shadowDistance.enabled" : "options.iris.shadowDistance.disabled"));

		return new OptionInstance<>("options.iris.shadowDistance",
			value -> tooltip,
			(caption, value) -> value <= 0
				? Options.genericValueLabel(caption, Component.translatable("options.off"))
				: Options.genericValueLabel(caption, Component.translatable("options.chunks", value)),
			new OptionInstance.IntRange(0, 32),
			IrisVideoSettings.getOverriddenShadowDistance(IrisVideoSettings.shadowDistance),
			value -> {
				IrisVideoSettings.shadowDistance = value;

				try {
					Iris.getIrisConfig().save();
				} catch (IOException e) {
					CeleritasShaders.logger().error("Failed to save the Iris config!", e);
				}
			});
	}
}
