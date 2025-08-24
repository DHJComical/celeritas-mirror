package org.taumc.celeritas.api.options.control;

import org.embeddedt.embeddium.impl.gui.framework.TextComponent;

public interface ControlValueFormatter {
    static ControlValueFormatter guiScale() {
        return (v) -> (v == 0) ? TextComponent.translatable("options.guiScale.auto") : TextComponent.literal(v + "x");
    }

    static ControlValueFormatter fpsLimit() {
        return (v) -> (v == 260) ? TextComponent.translatable("options.framerateLimit.max") : TextComponent.translatable("options.framerate", v);
    }

    static ControlValueFormatter brightness() {
        return (v) -> {
            if (v == 0) {
                return TextComponent.translatable("options.gamma.min");
            } else if (v == 100) {
                return TextComponent.translatable("options.gamma.max");
            } else {
                return TextComponent.literal(v + "%");
            }
        };
    }

    static ControlValueFormatter biomeBlend() {
        return (v) -> (v == 0) ? TextComponent.translatable("gui.none") : TextComponent.translatable("sodium.options.biome_blend.value", v);
    }

    TextComponent format(int value);

    static ControlValueFormatter translateVariable(String key) {
        return (v) -> TextComponent.translatable(key, v);
    }

    static ControlValueFormatter percentage() {
        return (v) -> TextComponent.literal(v + "%");
    }

    static ControlValueFormatter multiplier() {
        return (v) -> TextComponent.literal(v + "x");
    }

    static ControlValueFormatter quantityOrDisabled(String name, String disableText) {
        return (v) -> TextComponent.literal(v == 0 ? disableText : v + " " + name);
    }

    static ControlValueFormatter number() {
        return (v) -> TextComponent.literal(String.valueOf(v));
    }
}
