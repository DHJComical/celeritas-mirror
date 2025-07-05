package org.embeddedt.embeddium.api.options.structure;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.embeddedt.embeddium.impl.gui.options.TextProvider;
import org.embeddedt.embeddium.impl.util.ComponentUtil;

public enum OptionImpact implements TextProvider {
    LOW(ChatFormatting.GREEN, "sodium.option_impact.low"),
    MEDIUM(ChatFormatting.YELLOW, "sodium.option_impact.medium"),
    HIGH(ChatFormatting.GOLD, "sodium.option_impact.high"),
    VARIES(ChatFormatting.WHITE, "sodium.option_impact.varies");

    private final Component text;

    OptionImpact(ChatFormatting color, String text) {
        this.text = ComponentUtil.translatable(text).withStyle(color);
    }

    @Override
    public Component getLocalizedName() {
        return this.text;
    }
}
