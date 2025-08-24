package org.taumc.celeritas.api.options.structure;

import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.embeddedt.embeddium.impl.gui.framework.TextFormattingStyle;
import org.embeddedt.embeddium.impl.gui.options.TextProvider;

public enum OptionImpact implements TextProvider {
    LOW(TextFormattingStyle.GREEN, "sodium.option_impact.low"),
    MEDIUM(TextFormattingStyle.YELLOW, "sodium.option_impact.medium"),
    HIGH(TextFormattingStyle.GOLD, "sodium.option_impact.high"),
    VARIES(TextFormattingStyle.WHITE, "sodium.option_impact.varies");

    private final TextComponent text;

    OptionImpact(TextFormattingStyle color, String text) {
        this.text = TextComponent.translatable(text).withStyle(color);
    }

    @Override
    public TextComponent getLocalizedName() {
        return this.text;
    }
}
