package org.embeddedt.embeddium.impl.gui.framework;

public interface FontMetricsProvider {
    default int getStringWidth(String str) {
        return getStringWidth(new TextComponent.Literal(str));
    }

    int getStringWidth(TextComponent component);

    String substrByWidth(String str, int maxWidth);

    int lineHeight();
}
