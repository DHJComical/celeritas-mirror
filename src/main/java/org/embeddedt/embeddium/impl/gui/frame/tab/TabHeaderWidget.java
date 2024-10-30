package org.embeddedt.embeddium.impl.gui.frame.tab;

import net.minecraft.client.renderer.RenderType;
import org.embeddedt.embeddium.impl.gui.widgets.FlatButtonWidget;
import org.embeddedt.embeddium.impl.loader.common.ModLogoUtil;
import org.embeddedt.embeddium.impl.util.Dim2i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.embeddedt.embeddium.impl.util.ResourceLocationUtil;

import java.util.Objects;

public class TabHeaderWidget extends FlatButtonWidget {
    private static final ResourceLocation FALLBACK_LOCATION = ResourceLocationUtil.make("textures/misc/unknown_pack.png");

    private final ResourceLocation logoTexture;

    public static MutableComponent getLabel(String modId) {
        return (switch(modId) {
            // TODO handle long mod names better, this is the only one we know of right now
            case "sspb" -> Component.literal("SSPB");
            default -> Tab.idComponent(modId);
        }).withStyle(s -> s.withUnderlined(true));
    }

    public TabHeaderWidget(Dim2i dim, String modId) {
        super(dim, getLabel(modId), () -> {});
        this.logoTexture = ModLogoUtil.registerLogo(modId);
    }

    @Override
    protected int getLeftAlignedTextOffset() {
        return super.getLeftAlignedTextOffset() + Minecraft.getInstance().font.lineHeight;
    }

    @Override
    protected boolean isHovered(int mouseX, int mouseY) {
        return false;
    }

    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        super.render(drawContext, mouseX, mouseY, delta);

        ResourceLocation icon = Objects.requireNonNullElse(this.logoTexture, FALLBACK_LOCATION);
        int fontHeight = Minecraft.getInstance().font.lineHeight;
        int imgY = this.dim.getCenterY() - (fontHeight / 2);
        drawContext.blit(/*? if >=1.21.2 {*/ /*RenderType::guiTextured, *//*?}*/ icon, this.dim.x() + 5, imgY, 0.0f, 0.0f, fontHeight, fontHeight, fontHeight, fontHeight);
    }
}
