package org.embeddedt.embeddium.impl.gui.frame.tab;

import org.embeddedt.embeddium.impl.gui.framework.DrawContext;
import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.embeddedt.embeddium.impl.gui.framework.TextFormattingStyle;
import org.embeddedt.embeddium.impl.gui.widgets.FlatButtonWidget;
import org.embeddedt.embeddium.impl.util.Dim2i;

import java.util.Objects;

public class TabHeaderWidget extends FlatButtonWidget {
    private static final String FALLBACK_TEXTURE = "textures/misc/unknown_pack.png";

    private final String modId;

    public TabHeaderWidget(Dim2i dim, String modId) {
        super(dim, TextComponent.literal(""), () -> {});
        this.modId = modId;
    }

    @Override
    protected int getLeftAlignedTextOffset(DrawContext drawContext) {
        return super.getLeftAlignedTextOffset(drawContext) + drawContext.lineHeight();
    }

    @Override
    protected boolean isHovered(int mouseX, int mouseY) {
        return false;
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        this.setLabel(drawContext.getFriendlyModName(modId).withStyle(TextFormattingStyle.UNDERLINE));
        super.render(drawContext, mouseX, mouseY, delta);
        String icon = Objects.requireNonNullElse(drawContext.getModLogoPath(modId), FALLBACK_TEXTURE);
        int fontHeight = drawContext.lineHeight();
        int imgY = this.dim.getCenterY() - (fontHeight / 2);
        drawContext.blitWholeImage(icon, this.dim.x() + 5, imgY, fontHeight, fontHeight);
    }
}
