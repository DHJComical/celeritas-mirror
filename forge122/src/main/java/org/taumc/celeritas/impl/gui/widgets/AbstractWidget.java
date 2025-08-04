package org.taumc.celeritas.impl.gui.widgets;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.text.ITextComponent;
import org.taumc.celeritas.impl.gui.compat.GuiGraphics;
import org.taumc.celeritas.impl.gui.compat.Renderable;
import org.taumc.celeritas.impl.gui.compat.Element;

public abstract class AbstractWidget implements Renderable, Element {
    protected final FontRenderer font;
    protected boolean focused;
    protected boolean hovered;

    protected AbstractWidget() {
        this.font = Minecraft.getMinecraft().fontRenderer;
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
    }

    protected void drawString(GuiGraphics drawContext, String str, int x, int y, int color) {
        drawContext.drawString(this.font, str, x, y, color);
    }

    protected void drawString(GuiGraphics drawContext, ITextComponent text, int x, int y, int color) {
        drawContext.drawString(this.font, text, x, y, color);
    }

    public boolean isHovered() {
        return this.hovered;
    }

    protected void drawRect(GuiGraphics drawContext, int x1, int y1, int x2, int y2, int color) {
        drawContext.fill(x1, y1, x2, y2, color);
    }

    protected void playClickSound() {
        Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    protected int getStringWidth(String text) {
        return this.font.getStringWidth(text);
    }

    protected int getStringWidth(ITextComponent text) {
        return this.font.getStringWidth(text.getFormattedText());
    }

    public static List<String> split(String string, int width) {
        return Minecraft.getMinecraft().fontRenderer.listFormattedStringToWidth(string, width);
    }

    public static List<String> split(ITextComponent component, int width) {
        return split(component.getFormattedText(), width);
    }

    public boolean isFocused() {
        return this.focused;
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    protected static boolean keySelected(int keyCode) {
        return keyCode == 57 || keyCode == 28;
    }

    protected void drawBorder(GuiGraphics drawContext, int x1, int y1, int x2, int y2, int color) {
        drawContext.fill(x1, y1, x2, y1 + 1, color);
        drawContext.fill(x1, y2 - 1, x2, y2, color);
        drawContext.fill(x1, y1, x1 + 1, y2, color);
        drawContext.fill(x2 - 1, y1, x2, y2, color);
    }
}
