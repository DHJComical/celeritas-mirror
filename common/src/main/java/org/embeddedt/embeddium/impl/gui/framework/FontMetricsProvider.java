package org.embeddedt.embeddium.impl.gui.framework;

import java.util.List;

public interface FontMetricsProvider {
    default int getStringWidth(String str) {
        return getStringWidth(new TextComponent.Literal(str));
    }

    int getStringWidth(TextComponent component);

    default String substrByWidth(String str, int maxWidth) {
        return substrByWidth(str, maxWidth, false);
    }

    /**
     * @param reverse take the substring from the end of {@code str} instead of the start
     */
    String substrByWidth(String str, int maxWidth, boolean reverse);

    List<TextComponent> split(TextComponent component, int maxWidth);

    String extractString(TextComponent component);

    int lineHeight();
}
