package org.embeddedt.embeddium.impl.gui.compat;

//? if <1.20 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.embeddedt.embeddium.impl.util.ComponentUtil;

public class GuiGraphics {
    public final PoseStack stack;

    public GuiGraphics(PoseStack stack) {
        this.stack = stack;
    }

    public GuiGraphics(Minecraft mc, MultiBufferSource bufferSource) {
        this(new PoseStack());
    }

    public PoseStack pose() {
        return this.stack;
    }

    public void flush() {

    }

    public int drawString(Font font, String str, int x, int y, int color) {
        return drawString(font, ComponentUtil.literal(str), x, y, color);
    }

    public int drawString(Font font, Component component, int x, int y, int color) {
        return drawString(font, component.getVisualOrderText(), x, y, color, true);
    }

    public int drawString(Font font, FormattedCharSequence sequence, int x, int y, int color) {
        return drawString(font, sequence, x, y, color, true);
    }

    public int drawString(Font font, FormattedCharSequence sequence, int x, int y, int color, boolean shadow) {
        if(shadow) {
            return font.drawShadow(stack, sequence, x, y, color);
        } else {
            return font.draw(stack, sequence, x, y, color);
        }
    }

    public void fill(int x1, int y1, int x2, int y2, int color) {
        Gui.fill(stack, x1, y1, x2, y2, color);
    }
}

*///?}