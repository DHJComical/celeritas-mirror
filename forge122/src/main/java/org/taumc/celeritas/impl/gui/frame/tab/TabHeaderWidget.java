package org.taumc.celeritas.impl.gui.frame.tab;

import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import org.taumc.celeritas.impl.gui.compat.GuiGraphics;
import org.taumc.celeritas.impl.gui.widgets.FlatButtonWidget;
import org.taumc.celeritas.impl.loader.common.ModLogoUtil;
import org.taumc.celeritas.impl.util.Dim2i;
import org.taumc.celeritas.impl.util.ResourceLocationUtil;

public class TabHeaderWidget extends FlatButtonWidget {
    private static final ResourceLocation FALLBACK_LOCATION = ResourceLocationUtil.make("textures/misc/unknown_pack.png");
    private final ResourceLocation logoTexture;

    public TabHeaderWidget(Dim2i dim, String modId) {
        super(dim, Tab.idComponent(modId), () -> {
        });
        this.logoTexture = ModLogoUtil.registerLogo(modId);
    }

    @Override
    protected int getLeftAlignedTextOffset() {
        return super.getLeftAlignedTextOffset() + Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT;
    }

    @Override
    protected boolean isHovered(int mouseX, int mouseY) {
        return false;
    }

    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        super.render(drawContext, mouseX, mouseY, delta);
        ResourceLocation icon = Objects.requireNonNullElse(this.logoTexture, FALLBACK_LOCATION);
        int fontHeight = Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT;
        int imgY = this.dim.getCenterY() - fontHeight / 2;
        drawContext.blit(icon, this.dim.x() + 5, imgY, 0.0F, 0.0F, fontHeight, fontHeight, fontHeight, fontHeight);
    }
}
