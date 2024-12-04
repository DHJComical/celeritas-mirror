package org.embeddedt.embeddium.impl.gui.options;

import com.mojang.blaze3d.platform.Monitor;
import com.mojang.blaze3d.platform.Window;
import org.embeddedt.embeddium.api.options.structure.OptionImpl;
import org.embeddedt.embeddium.impl.gui.SodiumGameOptionPages;
import org.embeddedt.embeddium.api.options.control.ControlValueFormatter;
import org.embeddedt.embeddium.api.options.control.SliderControl;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.embeddedt.embeddium.api.options.structure.StandardOptions;
import org.embeddedt.embeddium.impl.util.ComponentUtil;

import java.util.Optional;

/**
 * Helper class to avoid breaking lambda offsets in main option pages class.
 */
public class FullscreenResolutionHelper {
    public static boolean isFullscreenResAlreadyAdded() {
        return false;
    }

    public static OptionImpl<?, ?> createFullScreenResolutionOption() {
        Window window = Minecraft.getInstance().getWindow();
        Monitor monitor = window.findBestMonitor();
        int maxMode;
        if (monitor != null) {
            maxMode = monitor.getModeCount() - 1;
        } else {
            maxMode = -1;
        }
        ControlValueFormatter formatter = value -> {
            if (monitor == null) {
                return ComponentUtil.translatable("options.fullscreen.unavailable");
            } else if (value == -1) {
                return ComponentUtil.translatable("options.fullscreen.current");
            } else {
                return ComponentUtil.literal(monitor.getMode(value).toString());
            }
        };
        return OptionImpl.createBuilder(int.class, SodiumGameOptionPages.getVanillaOpts())
                .setId(StandardOptions.Option.FULLSCREEN_RESOLUTION)
                .setName(ComponentUtil.translatable("options.fullscreen.resolution"))
                .setTooltip(ComponentUtil.translatable("embeddium.options.fullscreen.resolution.tooltip"))
                .setControl(option -> new SliderControl(option, -1, maxMode, 1, formatter))
                .setBinding((opts, value) -> {
                    if (monitor != null) {
                        window.setPreferredFullscreenVideoMode(value == -1 ? Optional.empty() : Optional.of(monitor.getMode(value)));
                        window.changeFullscreenVideoMode();
                    }
                }, (opts) -> monitor != null ? window.getPreferredFullscreenVideoMode().map(monitor::getVideoModeIndex).orElse(-1) : -1)
                .build();
    }
}
