package org.embeddedt.embeddium.impl.gui.frame.tab;

import org.embeddedt.embeddium.impl.gui.framework.DrawContext;
import org.embeddedt.embeddium.impl.gui.widgets.FlatButtonWidget;
import org.embeddedt.embeddium.impl.loader.common.ModLogoUtil;
import org.embeddedt.embeddium.impl.util.Dim2i;
import net.minecraft.client.Minecraft;

import java.util.Objects;

public class TabHeaderWidget extends FlatButtonWidget {
    private static final String FALLBACK_TEXTURE = "textures/misc/unknown_pack.png";

    private final String logoTexture;

    public TabHeaderWidget(Dim2i dim, String modId) {
        super(dim, Tab.idComponent(modId), () -> {});
        this.logoTexture = ModLogoUtil.registerLogo(modId).toString();
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
        super.render(drawContext, mouseX, mouseY, delta);
        String icon = Objects.requireNonNullElse(this.logoTexture, FALLBACK_TEXTURE);
        int fontHeight = Minecraft.getInstance().font.lineHeight;
        int imgY = this.dim.getCenterY() - (fontHeight / 2);
        drawContext.blitWholeImage(icon, this.dim.x() + 5, imgY, fontHeight, fontHeight);
    }
}
