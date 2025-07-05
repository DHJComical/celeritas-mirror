package org.embeddedt.embeddium.api.options.control;

import net.minecraft.network.chat.Component;
import org.embeddedt.embeddium.impl.util.ComponentUtil;

public interface ControlValueFormatter {
    static ControlValueFormatter guiScale() {
        return (v) -> (v == 0) ? ComponentUtil.translatable("options.guiScale.auto") : ComponentUtil.literal(v + "x");
    }

    static ControlValueFormatter fpsLimit() {
        return (v) -> (v == 260) ? ComponentUtil.translatable("options.framerateLimit.max") : ComponentUtil.translatable("options.framerate", v);
    }

    static ControlValueFormatter brightness() {
        return (v) -> {
            if (v == 0) {
                return ComponentUtil.translatable("options.gamma.min");
            } else if (v == 100) {
                return ComponentUtil.translatable("options.gamma.max");
            } else {
                return ComponentUtil.literal(v + "%");
            }
        };
    }

    static ControlValueFormatter biomeBlend() {
        return (v) -> (v == 0) ? ComponentUtil.translatable("gui.none") : ComponentUtil.translatable("sodium.options.biome_blend.value", v);
    }

    Component format(int value);

    static ControlValueFormatter translateVariable(String key) {
        return (v) -> ComponentUtil.translatable(key, v);
    }

    static ControlValueFormatter percentage() {
        return (v) -> ComponentUtil.literal(v + "%");
    }

    static ControlValueFormatter multiplier() {
        return (v) -> ComponentUtil.literal(v + "x");
    }

    static ControlValueFormatter quantityOrDisabled(String name, String disableText) {
        return (v) -> ComponentUtil.literal(v == 0 ? disableText : v + " " + name);
    }

    static ControlValueFormatter number() {
        return (v) -> ComponentUtil.literal(String.valueOf(v));
    }
}
