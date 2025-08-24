package org.embeddedt.embeddium.impl.gui.framework;

import java.util.List;

public interface FontMetricsProvider {
    default int getStringWidth(String str) {
        return getStringWidth(new TextComponent.Literal(str));
    }

    int getStringWidth(TextComponent component);

    String substrByWidth(String str, int maxWidth);

    List<TextComponent> split(TextComponent component, int maxWidth);

    String extractString(TextComponent component);

    int lineHeight();
}
