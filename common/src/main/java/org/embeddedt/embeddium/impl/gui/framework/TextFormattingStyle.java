package org.embeddedt.embeddium.impl.gui.framework;

public enum TextFormattingStyle {
    BLACK,
    DARK_BLUE,
    DARK_GREEN,
    DARK_AQUA,
    DARK_RED,
    DARK_PURPLE,
    GOLD,
    GRAY,
    DARK_GRAY,
    BLUE,
    GREEN,
    AQUA,
    RED,
    LIGHT_PURPLE,
    YELLOW,
    WHITE,
    STRIKETHROUGH,
    UNDERLINE,
    ITALIC;

    public boolean isColor() {
        return this.ordinal() < 16;
    }
}
