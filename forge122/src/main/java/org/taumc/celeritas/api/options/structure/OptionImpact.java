package org.taumc.celeritas.api.options.structure;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import org.taumc.celeritas.impl.gui.options.TextProvider;
import org.taumc.celeritas.impl.util.ComponentUtil;

public enum OptionImpact implements TextProvider {
    LOW(TextFormatting.GREEN, "sodium.option_impact.low"),
    MEDIUM(TextFormatting.YELLOW, "sodium.option_impact.medium"),
    HIGH(TextFormatting.GOLD, "sodium.option_impact.high"),
    VARIES(TextFormatting.WHITE, "sodium.option_impact.varies");

    private final ITextComponent text;

    OptionImpact(TextFormatting color, String text) {
        this.text = ComponentUtil.translatable(text).setStyle((new Style()).setColor(color));
    }

    @Override
    public ITextComponent getLocalizedName() {
        return this.text;
    }
}
